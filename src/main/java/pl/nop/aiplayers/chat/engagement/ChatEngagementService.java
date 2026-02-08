package pl.nop.aiplayers.chat.engagement;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.nop.aiplayers.AIPlayersPlugin;
import pl.nop.aiplayers.ai.controller.PlannerSettings;
import pl.nop.aiplayers.chat.AIChatService;
import pl.nop.aiplayers.logging.AIPlayersFileLogger;
import pl.nop.aiplayers.manager.AIPlayerManager;
import pl.nop.aiplayers.model.AIPlayerSession;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class ChatEngagementService {

    private final Plugin plugin;
    private final AIChatService chatService;
    private final AIPlayerManager aiPlayerManager;
    private ChatEngagementConfig config;
    private final Gson gson;
    private HttpClient httpClient;
    private final AtomicReference<Long> nextEngageAtMillis;
    private final AtomicLong lastAttemptMillis;
    private final AtomicReference<Long> nextBot2BotEngageAtMillis;
    private final AtomicLong lastBot2BotAttemptMillis;
    private final AtomicLong bot2BotSequence;

    public ChatEngagementService(Plugin plugin, AIChatService chatService, AIPlayerManager aiPlayerManager,
                                 ChatEngagementConfig config) {
        this.plugin = plugin;
        this.chatService = chatService;
        this.aiPlayerManager = aiPlayerManager;
        this.config = config;
        this.gson = new Gson();
        this.httpClient = buildHttpClient(config);
        this.nextEngageAtMillis = new AtomicReference<>();
        this.lastAttemptMillis = new AtomicLong(0L);
        this.nextBot2BotEngageAtMillis = new AtomicReference<>();
        this.lastBot2BotAttemptMillis = new AtomicLong(0L);
        this.bot2BotSequence = new AtomicLong(0L);
    }

    public void tick(long nowMillis) {
        if (config.isBot2BotEnabled()) {
            tickBot2Bot(nowMillis);
        }
        if (!config.isEnabled()) {
            return;
        }
        long lastChatUpdate = chatService.getLastChatUpdateMillis();
        if (!shouldSchedule(nowMillis, lastChatUpdate)) {
            return;
        }
        if (nowMillis < nextEngageAtMillis.get()) {
            return;
        }
        lastAttemptMillis.set(nowMillis);
        Optional<AIPlayerSession> bot = selectOnlineBot();
        if (bot.isEmpty()) {
            scheduleNext(nowMillis);
            return;
        }
        Optional<Player> targetPlayer = selectRandomHumanPlayer();
        if (targetPlayer.isEmpty()) {
            scheduleNext(nowMillis);
            return;
        }
        EngagementRequest request = buildRequest(bot.get(), targetPlayer.get());
        if (request == null) {
            scheduleNext(nowMillis);
            return;
        }
        sendRequest(bot.get(), request, nowMillis);
    }

    public void updateConfig(ChatEngagementConfig newConfig) {
        if (newConfig == null) {
            return;
        }
        this.config = newConfig;
        this.httpClient = buildHttpClient(newConfig);
        this.nextEngageAtMillis.set(0L);
        this.lastAttemptMillis.set(0L);
        this.nextBot2BotEngageAtMillis.set(0L);
        this.lastBot2BotAttemptMillis.set(0L);
        this.bot2BotSequence.set(0L);
    }

    private HttpClient buildHttpClient(ChatEngagementConfig currentConfig) {
        return HttpClient.newBuilder()
                .connectTimeout(currentConfig.getConnectTimeout())
                .build();
    }

    private boolean shouldSchedule(long nowMillis, long lastChatUpdate) {
        Long scheduled = nextEngageAtMillis.get();
        if (scheduled == null || scheduled == 0L) {
            scheduleNext(nowMillis);
            return true;
        }
        if (lastChatUpdate == 0L) {
            return true;
        }
        if (lastChatUpdate > lastAttemptMillis.get()) {
            scheduleNext(lastChatUpdate);
            return false;
        }
        return true;
    }

    private boolean shouldScheduleBot2Bot(long nowMillis, long lastChatUpdate) {
        Long scheduled = nextBot2BotEngageAtMillis.get();
        if (scheduled == null || scheduled == 0L) {
            scheduleNextBot2Bot(nowMillis);
            return true;
        }
        if (lastChatUpdate == 0L) {
            return true;
        }
        if (lastChatUpdate > lastBot2BotAttemptMillis.get()) {
            scheduleNextBot2Bot(lastChatUpdate);
            return false;
        }
        return true;
    }

    private void scheduleNext(long baseMillis) {
        int minSeconds = config.getMinEmptyChatSeconds();
        int maxSeconds = config.getMaxEmptyChatSeconds();
        int delaySeconds = minSeconds == maxSeconds
                ? minSeconds
                : ThreadLocalRandom.current().nextInt(minSeconds, maxSeconds + 1);
        long next = baseMillis + delaySeconds * 1000L;
        nextEngageAtMillis.set(next);
        logToFile("Next chat engagement attempt scheduled in " + delaySeconds + "s.");
    }

    private void scheduleNextBot2Bot(long baseMillis) {
        int minSeconds = config.getMinBot2BotEmptyChatSeconds();
        int maxSeconds = config.getMaxBot2BotEmptyChatSeconds();
        int delaySeconds = minSeconds == maxSeconds
                ? minSeconds
                : ThreadLocalRandom.current().nextInt(minSeconds, maxSeconds + 1);
        long next = baseMillis + delaySeconds * 1000L;
        nextBot2BotEngageAtMillis.set(next);
        logToFile("Next bot2bot chat engagement attempt scheduled in " + delaySeconds + "s.");
    }

    private Optional<AIPlayerSession> selectOnlineBot() {
        List<AIPlayerSession> sessions = aiPlayerManager.getAllSessions().stream()
                .filter(session -> session.getNpcHandle().getLocation() != null)
                .sorted(Comparator.comparing(session -> session.getProfile().getName()))
                .toList();
        if (sessions.isEmpty()) {
            return Optional.empty();
        }
        int idx = ThreadLocalRandom.current().nextInt(sessions.size());
        return Optional.ofNullable(sessions.get(idx));
    }

    private Optional<Player> selectRandomHumanPlayer() {
        List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (aiPlayerManager.getProfile(player.getName()) != null) {
                continue;
            }
            players.add(player);
        }
        if (players.isEmpty()) {
            return Optional.empty();
        }
        int idx = ThreadLocalRandom.current().nextInt(players.size());
        return Optional.of(players.get(idx));
    }

    private EngagementRequest buildRequest(AIPlayerSession botSession, Player target) {
        String baseUrl = config.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            plugin.getLogger().warning("Chat engagement enabled but base-url is empty. Skipping request.");
            return null;
        }
        EngagementRequest request = new EngagementRequest();
        request.requestId = UUID.randomUUID().toString();
        request.timeMs = System.currentTimeMillis();
        request.server = buildServerInfo();
        request.bots = List.of(buildBotInfo(botSession));
        request.chat = buildPlayerChatHistory();
        request.settings = buildDefaultPlannerSettings();
        request.examplePrompt = "Napisz krótką wiadomość angażującą gracza/bota o nicku " + target.getName() + ".";
        request.targetPlayer = target.getName();
        return request;
    }

    private List<ChatLine> buildPlayerChatHistory() {
        List<AIChatService.ChatEntry> entries = chatService.getChatEntriesSnapshot();
        if (entries.isEmpty()) {
            return List.of();
        }
        List<ChatLine> history = new ArrayList<>();
        for (int i = entries.size() - 1; i >= 0 && history.size() < config.getChatHistoryLimit(); i--) {
            AIChatService.ChatEntry entry = entries.get(i);
            if (entry.getSenderType() != AIChatService.ChatSenderType.PLAYER) {
                continue;
            }
            ChatLine line = new ChatLine();
            line.tsMs = entry.getTimestampMillis();
            line.sender = entry.getSender();
            line.senderType = "PLAYER";
            line.message = entry.getMessage();
            history.add(line);
        }
        List<ChatLine> trimmed = new ArrayList<>(history);
        java.util.Collections.reverse(trimmed);
        return trimmed;
    }

    private void sendRequest(AIPlayerSession botSession, EngagementRequest request, long nowMillis) {
        String payload = gson.toJson(request);
        String targetUrl = config.getBaseUrl() + config.getEngagementPath();
        long startMillis = System.currentTimeMillis();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(config.getRequestTimeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        logToFile("Sending engagement request " + request.requestId + " to " + targetUrl
                + " for bot=" + botSession.getProfile().getName() + ", target=" + request.targetPlayer);
        logToFile("Engagement request " + request.requestId + " payload: " + payload);
        logToFile("Engagement request " + request.requestId + " timeouts: connect="
                + config.getConnectTimeout().toMillis() + "ms, request=" + config.getRequestTimeout().toMillis() + "ms");

        httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    long durationMillis = System.currentTimeMillis() - startMillis;
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        plugin.getLogger().warning("Engagement API responded with status " + response.statusCode());
                        logToFile("Engagement response " + request.requestId + " status " + response.statusCode()
                                + ", durationMs=" + durationMillis
                                + ", payload=" + response.body());
                        return null;
                    }
                    logToFile("Engagement response " + request.requestId + " durationMs=" + durationMillis
                            + ", payload=" + response.body());
                    return gson.fromJson(response.body(), PlannerResponse.class);
                })
                .thenAccept(response -> handlePlannerResponse(botSession, response))
                .exceptionally(ex -> {
                    String details = describeException(ex);
                    long durationMillis = System.currentTimeMillis() - startMillis;
                    String message = "Engagement API request failed after " + durationMillis + "ms to " + targetUrl
                            + " for request " + request.requestId + ": " + details;
                    plugin.getLogger().warning(message);
                    logToFile(message);
                    logToFile("Engagement request " + request.requestId + " payload (failure): " + payload);
                    return null;
                })
                .whenComplete((ignored, throwable) -> scheduleNext(nowMillis));
    }

    private void handlePlannerResponse(AIPlayerSession botSession, PlannerResponse response) {
        String message = extractPlannerMessage(botSession, response);
        if (message == null || message.isBlank()) {
            return;
        }
        chatService.sendChatMessage(botSession, message);
        logToFile("Engagement message sent by " + botSession.getProfile().getName() + ": " + message);
    }

    private String extractPlannerMessage(AIPlayerSession botSession, PlannerResponse response) {
        if (response == null || response.actions == null || response.actions.isEmpty()) {
            return null;
        }
        String botId = botSession.getProfile().getUuid().toString();
        PlannedAction action = response.actions.stream()
                .filter(item -> botId.equals(item.botId))
                .findFirst()
                .orElse(null);
        if (action == null || action.message == null || action.message.isBlank()) {
            return null;
        }
        return sanitizeMessage(action.message, botSession.getProfile().getName());
    }

    private String sanitizeMessage(String message, String botName) {
        if (message == null) {
            return "";
        }
        String trimmed = message.trim();
        if (trimmed.toLowerCase().startsWith("[bot]")) {
            trimmed = trimmed.substring(5).trim();
        }
        if (botName != null && !botName.isBlank()) {
            String prefix = botName + ":";
            if (trimmed.startsWith(prefix)) {
                trimmed = trimmed.substring(prefix.length()).trim();
            }
        }
        return pl.nop.aiplayers.chat.ChatMessageSanitizer.sanitizeOutgoing(trimmed);
    }

    private void tickBot2Bot(long nowMillis) {
        long lastChatUpdate = chatService.getLastChatUpdateMillis();
        if (!shouldScheduleBot2Bot(nowMillis, lastChatUpdate)) {
            return;
        }
        Long scheduled = nextBot2BotEngageAtMillis.get();
        if (scheduled == null || nowMillis < scheduled) {
            return;
        }
        lastBot2BotAttemptMillis.set(nowMillis);
        List<AIPlayerSession> bots = selectOnlineBots();
        if (bots.size() < 2) {
            scheduleNextBot2Bot(nowMillis);
            return;
        }
        AIPlayerSession initiator = bots.get(0);
        AIPlayerSession target = bots.get(1);
        Bot2BotRequest request = buildBot2BotRequest(initiator, target);
        if (request == null) {
            scheduleNextBot2Bot(nowMillis);
            return;
        }
        sendBot2BotRequest(initiator, request, nowMillis);
    }

    private List<AIPlayerSession> selectOnlineBots() {
        List<AIPlayerSession> sessions = aiPlayerManager.getAllSessions().stream()
                .filter(session -> session.getNpcHandle().getLocation() != null)
                .sorted(Comparator.comparing(session -> session.getProfile().getName()))
                .toList();
        if (sessions.size() <= 2) {
            return sessions;
        }
        int first = ThreadLocalRandom.current().nextInt(sessions.size());
        int second = first;
        while (second == first) {
            second = ThreadLocalRandom.current().nextInt(sessions.size());
        }
        List<AIPlayerSession> selected = new ArrayList<>();
        selected.add(sessions.get(first));
        selected.add(sessions.get(second));
        return selected;
    }

    private Bot2BotRequest buildBot2BotRequest(AIPlayerSession botSession, AIPlayerSession targetSession) {
        String baseUrl = config.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            plugin.getLogger().warning("Bot2Bot engagement enabled but base-url is empty. Skipping request.");
            return null;
        }
        Bot2BotRequest request = new Bot2BotRequest();
        request.requestId = UUID.randomUUID().toString();
        request.timeMs = System.currentTimeMillis();
        request.server = buildServerInfo();
        request.bots = List.of(buildBotInfo(botSession), buildBotInfo(targetSession));
        request.chat = buildBotChatHistory();
        request.settings = buildBot2BotSettings();
        request.examplePrompt = "Napisz krótką, zaczepną wiadomość do gracza/bota o nicku "
                + targetSession.getProfile().getName() + ".";
        request.targetPlayer = targetSession.getProfile().getName();
        return request;
    }

    private ServerInfo buildServerInfo() {
        ServerInfo server = new ServerInfo();
        server.serverId = config.getServerId();
        server.mode = config.getServerMode();
        server.onlinePlayers = Bukkit.getOnlinePlayers().size();
        return server;
    }

    private BotInfo buildBotInfo(AIPlayerSession session) {
        BotInfo bot = new BotInfo();
        bot.botId = session.getProfile().getUuid().toString();
        bot.name = session.getProfile().getName();
        bot.online = session.getNpcHandle().getLocation() != null;
        bot.cooldownMs = 0;
        bot.persona = buildPersona(session);
        return bot;
    }

    private Persona buildPersona(AIPlayerSession session) {
        Persona persona = new Persona();
        persona.language = config.getPersonaLanguage();
        persona.tone = config.getPersonaTone();
        persona.styleTags = new ArrayList<>(config.getPersonaStyleTags());
        persona.avoidTopics = new ArrayList<>(config.getPersonaAvoidTopics());
        persona.knowledgeLevel = config.getPersonaKnowledgeLevel();
        String overrideLanguage = session.getProfile().getMetadata().get("persona.language");
        if (overrideLanguage != null && !overrideLanguage.isBlank()) {
            persona.language = overrideLanguage;
        }
        String overrideTone = session.getProfile().getMetadata().get("persona.tone");
        if (overrideTone != null && !overrideTone.isBlank()) {
            persona.tone = overrideTone;
        }
        String overrideKnowledge = session.getProfile().getMetadata().get("persona.knowledge-level");
        if (overrideKnowledge != null && !overrideKnowledge.isBlank()) {
            persona.knowledgeLevel = overrideKnowledge;
        }
        String overrideStyle = session.getProfile().getMetadata().get("persona.style-tags");
        if (overrideStyle != null && !overrideStyle.isBlank()) {
            persona.styleTags = splitTags(overrideStyle);
        }
        String overrideAvoid = session.getProfile().getMetadata().get("persona.avoid-topics");
        if (overrideAvoid != null && !overrideAvoid.isBlank()) {
            persona.avoidTopics = splitTags(overrideAvoid);
        }
        return persona;
    }

    private List<String> splitTags(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
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

    private List<ChatLine> buildBotChatHistory() {
        List<AIChatService.ChatEntry> entries = chatService.getChatEntriesSnapshot();
        if (entries.isEmpty()) {
            return List.of();
        }
        List<ChatLine> history = new ArrayList<>();
        for (int i = entries.size() - 1; i >= 0 && history.size() < config.getChatHistoryLimit(); i--) {
            AIChatService.ChatEntry entry = entries.get(i);
            if (entry.getSenderType() != AIChatService.ChatSenderType.BOT) {
                continue;
            }
            ChatLine line = new ChatLine();
            line.tsMs = entry.getTimestampMillis();
            line.sender = entry.getSender();
            line.senderType = "BOT";
            line.message = sanitizeChatLine(entry.getMessage(), entry.getSender());
            history.add(line);
        }
        List<ChatLine> trimmed = new ArrayList<>(history);
        java.util.Collections.reverse(trimmed);
        return trimmed;
    }

    private String sanitizeChatLine(String message, String sender) {
        if (message == null) {
            return "";
        }
        return sanitizeMessage(message, sender);
    }

    private PlannerSettings buildDefaultPlannerSettings() {
        return new PlannerSettings(
                config.getPlannerMaxActions(),
                config.getPlannerMinDelayMs(),
                config.getPlannerMaxDelayMs(),
                config.getPlannerGlobalSilenceChance(),
                config.getPlannerReplyChance()
        );
    }

    private PlannerSettings buildBot2BotSettings() {
        double silence = config.getBot2BotBaseSilenceChance();
        long sequence = bot2BotSequence.getAndIncrement();
        silence = clampChance(silence + (sequence * config.getBot2BotSilenceMultiplier()));
        return new PlannerSettings(
                config.getPlannerMaxActions(),
                config.getPlannerMinDelayMs(),
                config.getPlannerMaxDelayMs(),
                silence,
                config.getPlannerReplyChance()
        );
    }

    private double clampChance(double value) {
        if (value < 0.0) {
            return 0.0;
        }
        if (value > 1.0) {
            return 1.0;
        }
        return value;
    }

    private void sendBot2BotRequest(AIPlayerSession botSession, Bot2BotRequest request, long nowMillis) {
        String payload = gson.toJson(request);
        String targetUrl = config.getBaseUrl() + config.getEngagementPath();
        long startMillis = System.currentTimeMillis();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(config.getRequestTimeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        logToFile("Sending bot2bot engagement request " + request.requestId + " to " + targetUrl
                + " for bot=" + botSession.getProfile().getName()
                + ", target=" + request.targetPlayer);
        logToFile("Bot2bot engagement request " + request.requestId + " payload: " + payload);
        logToFile("Bot2bot engagement request " + request.requestId + " timeouts: connect="
                + config.getConnectTimeout().toMillis() + "ms, request=" + config.getRequestTimeout().toMillis() + "ms");

        httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    long durationMillis = System.currentTimeMillis() - startMillis;
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        plugin.getLogger().warning("Bot2bot engagement API responded with status " + response.statusCode());
                        logToFile("Bot2bot engagement response " + request.requestId + " status " + response.statusCode()
                                + ", durationMs=" + durationMillis
                                + ", payload=" + response.body());
                        return null;
                    }
                    logToFile("Bot2bot engagement response " + request.requestId + " durationMs=" + durationMillis
                            + ", payload=" + response.body());
                    return gson.fromJson(response.body(), PlannerResponse.class);
                })
                .thenAccept(response -> handlePlannerResponse(botSession, response))
                .exceptionally(ex -> {
                    String details = describeException(ex);
                    long durationMillis = System.currentTimeMillis() - startMillis;
                    String message = "Bot2bot engagement API request failed after " + durationMillis + "ms to " + targetUrl
                            + " for request " + request.requestId + ": " + details;
                    plugin.getLogger().warning(message);
                    logToFile(message);
                    logToFile("Bot2bot engagement request " + request.requestId + " payload (failure): " + payload);
                    return null;
                })
                .whenComplete((ignored, throwable) -> scheduleNextBot2Bot(nowMillis));
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

    private String describeException(Throwable ex) {
        Throwable root = ex;
        if (ex instanceof CompletionException && ex.getCause() != null) {
            root = ex.getCause();
        }
        StringBuilder details = new StringBuilder(root.getClass().getSimpleName());
        String message = root.getMessage();
        if (message != null && !message.isBlank()) {
            details.append(": ").append(message);
        }
        Throwable cause = root.getCause();
        if (cause != null && cause != root) {
            details.append(" (cause: ").append(cause.getClass().getSimpleName());
            String causeMessage = cause.getMessage();
            if (causeMessage != null && !causeMessage.isBlank()) {
                details.append(": ").append(causeMessage);
            }
            details.append(")");
        }
        return details.toString();
    }

    private static class EngagementRequest {
        @SerializedName("request_id")
        private String requestId;
        @SerializedName("time_ms")
        private long timeMs;
        private ServerInfo server;
        private List<BotInfo> bots;
        private List<ChatLine> chat;
        private PlannerSettings settings;
        @SerializedName("example_prompt")
        private String examplePrompt;
        @SerializedName("target_player")
        private String targetPlayer;
    }

    private static class PlannerResponse {
        @SerializedName("request_id")
        private String requestId;
        private List<PlannedAction> actions;
    }

    private static class PlannedAction {
        @SerializedName("bot_id")
        private String botId;
        @SerializedName("send_after_ms")
        private long sendAfterMs;
        private String message;
        private String visibility;
    }

    private static class Bot2BotRequest {
        @SerializedName("request_id")
        private String requestId;
        @SerializedName("time_ms")
        private long timeMs;
        private ServerInfo server;
        private List<BotInfo> bots;
        private List<ChatLine> chat;
        private PlannerSettings settings;
        @SerializedName("example_prompt")
        private String examplePrompt;
        @SerializedName("target_player")
        private String targetPlayer;
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
}
