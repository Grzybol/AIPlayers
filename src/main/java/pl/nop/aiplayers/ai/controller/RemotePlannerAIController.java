package pl.nop.aiplayers.ai.controller;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import pl.nop.aiplayers.AIPlayersPlugin;
import pl.nop.aiplayers.ai.Action;
import pl.nop.aiplayers.ai.Perception;
import pl.nop.aiplayers.chat.AIChatService;
import pl.nop.aiplayers.logging.AIPlayersFileLogger;
import pl.nop.aiplayers.manager.AIPlayerManager;
import pl.nop.aiplayers.model.AIPlayerProfile;
import pl.nop.aiplayers.model.AIPlayerSession;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RemotePlannerAIController implements AIController {

    private final Plugin plugin;
    private final AIChatService chatService;
    private final AIPlayerManager manager;
    private final Gson gson;
    private final HttpClient httpClient;
    private final RemotePlannerConfig config;
    private final ConcurrentHashMap<UUID, Long> lastRequestMillis;

    public RemotePlannerAIController(Plugin plugin, AIChatService chatService, AIPlayerManager manager, RemotePlannerConfig config) {
        this.plugin = plugin;
        this.chatService = chatService;
        this.manager = manager;
        this.config = config;
        this.gson = new Gson();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.getConnectTimeout())
                .build();
        this.lastRequestMillis = new ConcurrentHashMap<>();
    }

    @Override
    public CompletableFuture<Action> decide(AIPlayerSession session, Perception perception) {
        if (!config.isEnabled()) {
            return CompletableFuture.completedFuture(Action.idle());
        }
        if (!shouldSendRequest(session)) {
            return CompletableFuture.completedFuture(Action.idle());
        }
        PlannerRequest request = buildRequest(session, perception);
        if (request == null) {
            logToFile("Planner request skipped: missing base URL for bot " + session.getProfile().getName());
            return CompletableFuture.completedFuture(Action.idle());
        }
        String payload = gson.toJson(request);
        String targetUrl = config.getBaseUrl() + config.getPlanPath();
        plugin.getLogger().info("Sending chat request to the server " + targetUrl + " for AIPlayer " + session.getProfile().getName());
        logToFile("Sending planner request " + request.requestId + " to " + targetUrl
                + " for bot=" + session.getProfile().getName()
                + ", chatLines=" + (request.chat == null ? 0 : request.chat.size()));
        logToFile("Planner request " + request.requestId + " payload: " + payload);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(config.getRequestTimeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        logToFile("Planner request " + request.requestId + " headers: Content-Type=application/json");

        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        plugin.getLogger().warning("Planner API responded with status " + response.statusCode());
                        logToFile("Planner API responded with status " + response.statusCode()
                                + " for request " + request.requestId);
                        logToFile("Planner response " + request.requestId + " payload: " + response.body());
                        return null;
                    }
                    logToFile("Planner API responded with status " + response.statusCode()
                            + " for request " + request.requestId
                            + ", payloadLength=" + response.body().length());
                    logToFile("Planner response " + request.requestId + " payload: " + response.body());
                    return gson.fromJson(response.body(), PlannerResponse.class);
                })
                .thenCompose(response -> toActionFuture(session, response))
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Planner API request failed: " + ex.getMessage());
                    logToFile("Planner API request failed for " + request.requestId + ": " + ex.getMessage());
                    return Action.idle();
                });
    }

    private boolean shouldSendRequest(AIPlayerSession session) {
        UUID botId = session.getProfile().getUuid();
        long now = System.currentTimeMillis();
        long lastSent = lastRequestMillis.getOrDefault(botId, 0L);
        long lastChatUpdate = chatService.getLastChatUpdateMillis();
        boolean chatUpdated = lastChatUpdate > lastSent;
        boolean intervalElapsed = now - lastSent >= config.getRequestIntervalMillis();
        if (chatUpdated || intervalElapsed) {
            lastRequestMillis.put(botId, now);
            return true;
        }
        return false;
    }

    private CompletableFuture<Action> toActionFuture(AIPlayerSession session, PlannerResponse response) {
        if (response == null || response.actions == null || response.actions.isEmpty()) {
            return CompletableFuture.completedFuture(Action.idle());
        }
        String botId = session.getProfile().getUuid().toString();
        PlannedAction planned = response.actions.stream()
                .filter(action -> Objects.equals(botId, action.botId))
                .findFirst()
                .orElse(null);
        if (planned == null || planned.message == null || planned.message.isBlank()) {
            logToFile("Planner response contained no chat action for bot " + session.getProfile().getName());
            return CompletableFuture.completedFuture(Action.idle());
        }
        if ("__SILENCE__".equals(planned.message)) {
            logToFile("Planner response instructed silence for bot " + session.getProfile().getName());
            return CompletableFuture.completedFuture(Action.idle());
        }
        long delay = Math.max(0, planned.sendAfterMs);
        String cleanedMessage = stripBotPrefix(planned.message, session.getProfile().getName());
        if (cleanedMessage.isBlank()) {
            logToFile("Planner response message was empty after cleanup for bot " + session.getProfile().getName());
            return CompletableFuture.completedFuture(Action.idle());
        }
        Action action = Action.say(cleanedMessage);
        logToFile("Planner response action for bot " + session.getProfile().getName()
                + ": message='" + cleanedMessage + "', sendAfterMs=" + planned.sendAfterMs);
        if (delay <= 0) {
            return CompletableFuture.completedFuture(action);
        }
        return CompletableFuture.supplyAsync(() -> action,
                CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS));
    }

    private PlannerRequest buildRequest(AIPlayerSession session, Perception perception) {
        String baseUrl = config.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        String requestId = UUID.randomUUID().toString();
        PlannerRequest request = new PlannerRequest();
        request.requestId = requestId;
        request.tick = perception.getServerTimeTicks();
        request.timeMs = System.currentTimeMillis();
        request.server = new ServerInfo();
        request.server.serverId = config.getServerId();
        request.server.mode = config.getServerMode();
        request.server.onlinePlayers = Bukkit.getOnlinePlayers().size();

        List<AIChatService.ChatEntry> chatEntries = chatService.getChatEntriesSnapshot();

        BotInfo bot = new BotInfo();
        bot.botId = session.getProfile().getUuid().toString();
        bot.name = session.getProfile().getName();
        bot.online = session.getNpcHandle().getLocation() != null;
        bot.cooldownMs = 0;
        bot.persona = buildPersona(session.getProfile(), detectLanguage(chatEntries));
        request.bots = Collections.singletonList(bot);

        request.chat = buildChat(chatEntries);
        request.settings = config.getSettings();
        return request;
    }

    private Persona buildPersona(AIPlayerProfile profile, String detectedLanguage) {
        Persona persona = new Persona();
        persona.language = detectedLanguage != null ? detectedLanguage : config.getPersonaLanguage();
        persona.tone = config.getPersonaTone();
        persona.styleTags = new ArrayList<>(config.getPersonaStyleTags());
        persona.avoidTopics = new ArrayList<>(config.getPersonaAvoidTopics());
        persona.knowledgeLevel = config.getPersonaKnowledgeLevel();

        String overrideLanguage = profile.getMetadata().get("persona.language");
        if (overrideLanguage != null && !overrideLanguage.isBlank()) {
            persona.language = overrideLanguage;
        }
        String overrideTone = profile.getMetadata().get("persona.tone");
        if (overrideTone != null && !overrideTone.isBlank()) {
            persona.tone = overrideTone;
        }
        String overrideKnowledge = profile.getMetadata().get("persona.knowledge-level");
        if (overrideKnowledge != null && !overrideKnowledge.isBlank()) {
            persona.knowledgeLevel = overrideKnowledge;
        }
        String overrideStyle = profile.getMetadata().get("persona.style-tags");
        if (overrideStyle != null && !overrideStyle.isBlank()) {
            persona.styleTags = splitTags(overrideStyle);
        }
        String overrideAvoid = profile.getMetadata().get("persona.avoid-topics");
        if (overrideAvoid != null && !overrideAvoid.isBlank()) {
            persona.avoidTopics = splitTags(overrideAvoid);
        }
        return persona;
    }

    private List<String> splitTags(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        String[] parts = value.split(",");
        List<String> tags = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                tags.add(trimmed);
            }
        }
        return tags;
    }

    private List<ChatLine> buildChat(List<AIChatService.ChatEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }
        int limit = config.getChatLimit();
        int start = Math.max(0, entries.size() - limit);
        List<ChatLine> chatLines = new ArrayList<>();
        for (int i = start; i < entries.size(); i++) {
            AIChatService.ChatEntry entry = entries.get(i);
            ChatLine line = new ChatLine();
            line.tsMs = entry.getTimestampMillis();
            line.sender = entry.getSender();
            line.senderType = manager.getProfile(entry.getSender()) != null ? "BOT" : "PLAYER";
            line.message = sanitizeChatMessage(line.sender, line.senderType, entry.getMessage());
            chatLines.add(line);
        }
        return chatLines;
    }

    private String sanitizeChatMessage(String sender, String senderType, String message) {
        if (message == null) {
            return "";
        }
        if (!"BOT".equals(senderType)) {
            return message;
        }
        return stripBotPrefix(message, sender);
    }

    private String stripBotPrefix(String message, String botName) {
        if (message == null) {
            return "";
        }
        String trimmed = message.trim();
        if (botName == null || botName.isBlank()) {
            return trimmed;
        }
        String prefix = ":" + botName + ":";
        while (trimmed.startsWith(prefix)) {
            trimmed = trimmed.substring(prefix.length()).trim();
        }
        return trimmed;
    }

    private String detectLanguage(List<AIChatService.ChatEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        int checked = 0;
        for (int i = entries.size() - 1; i >= 0 && checked < config.getChatLimit(); i--) {
            AIChatService.ChatEntry entry = entries.get(i);
            if (manager.getProfile(entry.getSender()) != null) {
                continue;
            }
            String message = entry.getMessage();
            if (message == null || message.isBlank()) {
                continue;
            }
            String lower = message.toLowerCase();
            if (containsPolishCharacters(lower) || containsPolishWords(lower)) {
                return "pl";
            }
            if (containsEnglishWords(lower)) {
                return "en";
            }
            checked++;
        }
        return null;
    }

    private boolean containsPolishCharacters(String text) {
        return text.contains("ą") || text.contains("ć") || text.contains("ę")
                || text.contains("ł") || text.contains("ń") || text.contains("ó")
                || text.contains("ś") || text.contains("ż") || text.contains("ź");
    }

    private boolean containsPolishWords(String text) {
        return text.contains("siemka") || text.contains("cześć") || text.contains("czesc")
                || text.contains("dzięki") || text.contains("dzieki") || text.contains("proszę")
                || text.contains("prosze") || text.contains("co tam") || text.contains("jakie")
                || text.contains("wiadomo") || text.contains("grac");
    }

    private boolean containsEnglishWords(String text) {
        return text.contains("hello") || text.contains("hi ") || text.contains("thanks")
                || text.contains("what ") || text.contains("how are") || text.contains("you ")
                || text.contains("newbie") || text.contains("learning");
    }

    private void logToFile(String message) {
        AIPlayersFileLogger fileLogger = getFileLogger();
        if (fileLogger != null) {
            fileLogger.info(message);
        }
    }

    private AIPlayersFileLogger getFileLogger() {
        if (plugin instanceof AIPlayersPlugin) {
            return ((AIPlayersPlugin) plugin).getFileLogger();
        }
        return null;
    }

    private static class PlannerRequest {
        @SerializedName("request_id")
        private String requestId;
        private ServerInfo server;
        private long tick;
        @SerializedName("time_ms")
        private long timeMs;
        private List<BotInfo> bots;
        private List<ChatLine> chat;
        private PlannerSettings settings;
    }

    private static class PlannerResponse {
        @SerializedName("request_id")
        private String requestId;
        private List<PlannedAction> actions;
    }

    private static class ServerInfo {
        @SerializedName("server_id")
        private String serverId;
        private String mode;
        @SerializedName("online_players")
        private int onlinePlayers;
    }

    private static class BotInfo {
        @SerializedName("bot_id")
        private String botId;
        private String name;
        private boolean online;
        @SerializedName("cooldown_ms")
        private int cooldownMs;
        private Persona persona;
    }

    private static class Persona {
        private String language;
        private String tone;
        @SerializedName("style_tags")
        private List<String> styleTags;
        @SerializedName("avoid_topics")
        private List<String> avoidTopics;
        @SerializedName("knowledge_level")
        private String knowledgeLevel;
    }

    private static class ChatLine {
        @SerializedName("ts_ms")
        private long tsMs;
        private String sender;
        @SerializedName("sender_type")
        private String senderType;
        private String message;
    }

    private static class PlannedAction {
        @SerializedName("bot_id")
        private String botId;
        @SerializedName("send_after_ms")
        private long sendAfterMs;
        private String message;
        private String visibility;
    }
}
