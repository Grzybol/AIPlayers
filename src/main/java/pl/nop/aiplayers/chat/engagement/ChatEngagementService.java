package pl.nop.aiplayers.chat.engagement;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.nop.aiplayers.AIPlayersPlugin;
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
    }

    public void tick(long nowMillis) {
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
        request.botName = botSession.getProfile().getName();
        request.botId = botSession.getProfile().getUuid().toString();
        request.targetPlayer = target.getName();
        request.chatHistory = buildChatHistory();
        request.examplePrompt = "Siema " + target.getName()
                + ", robisz coś ciekawego? Nudzi mi się i chcę pogadać!";
        return request;
    }

    private List<String> buildChatHistory() {
        List<AIChatService.ChatEntry> entries = chatService.getChatEntriesSnapshot();
        if (entries.isEmpty()) {
            return List.of();
        }
        List<String> history = new ArrayList<>();
        for (AIChatService.ChatEntry entry : entries) {
            if (entry.getSenderType() != AIChatService.ChatSenderType.PLAYER) {
                continue;
            }
            history.add(entry.getRawLine());
        }
        if (history.isEmpty()) {
            return history;
        }
        int limit = config.getChatHistoryLimit();
        int start = Math.max(0, history.size() - limit);
        return new ArrayList<>(history.subList(start, history.size()));
    }

    private void sendRequest(AIPlayerSession botSession, EngagementRequest request, long nowMillis) {
        String payload = gson.toJson(request);
        String targetUrl = config.getBaseUrl() + config.getEngagePath();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(config.getRequestTimeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();
        logToFile("Sending engagement request " + request.requestId + " to " + targetUrl
                + " for bot=" + request.botName + ", target=" + request.targetPlayer);
        logToFile("Engagement request " + request.requestId + " payload: " + payload);

        httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        plugin.getLogger().warning("Engagement API responded with status " + response.statusCode());
                        logToFile("Engagement response " + request.requestId + " status " + response.statusCode()
                                + ", payload=" + response.body());
                        return null;
                    }
                    logToFile("Engagement response " + request.requestId + " payload=" + response.body());
                    return gson.fromJson(response.body(), EngagementResponse.class);
                })
                .thenAccept(response -> handleResponse(botSession, response))
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Engagement API request failed: " + ex.getMessage());
                    logToFile("Engagement API request failed for " + request.requestId + ": " + ex.getMessage());
                    return null;
                })
                .whenComplete((ignored, throwable) -> scheduleNext(nowMillis));
    }

    private void handleResponse(AIPlayerSession botSession, EngagementResponse response) {
        if (response == null || response.message == null) {
            return;
        }
        String message = sanitizeMessage(response.message, botSession.getProfile().getName());
        if (message.isBlank()) {
            return;
        }
        chatService.sendChatMessage(botSession, message);
        logToFile("Engagement message sent by " + botSession.getProfile().getName() + ": " + message);
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

    private static class EngagementRequest {
        @SerializedName("request_id")
        private String requestId;
        @SerializedName("time_ms")
        private long timeMs;
        @SerializedName("bot_id")
        private String botId;
        @SerializedName("bot_name")
        private String botName;
        @SerializedName("target_player")
        private String targetPlayer;
        @SerializedName("chat_history")
        private List<String> chatHistory;
        @SerializedName("example_prompt")
        private String examplePrompt;
    }

    private static class EngagementResponse {
        @SerializedName("message")
        private String message;
        @SerializedName("target_player")
        private String targetPlayer;
        @SerializedName("bot_id")
        private String botId;
    }
}
