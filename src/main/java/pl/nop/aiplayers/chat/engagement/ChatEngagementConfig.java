package pl.nop.aiplayers.chat.engagement;

import org.bukkit.configuration.file.FileConfiguration;

import java.time.Duration;

public class ChatEngagementConfig {

    private final boolean enabled;
    private final int minEmptyChatSeconds;
    private final int maxEmptyChatSeconds;
    private final String baseUrl;
    private final String engagePath;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final int chatHistoryLimit;

    public ChatEngagementConfig(FileConfiguration config) {
        this.enabled = config.getBoolean("chat.engagement.engage-players-on-chat", false);
        int minSeconds = Math.max(1, config.getInt("chat.engagement.min-empty-chat-time-to-engage-in-seconds", 120));
        int maxSeconds = Math.max(minSeconds, config.getInt("chat.engagement.max-empty-chat-time-to-engage-in-seconds", 300));
        this.minEmptyChatSeconds = minSeconds;
        this.maxEmptyChatSeconds = maxSeconds;
        this.baseUrl = config.getString("chat.engagement.base-url", "");
        this.engagePath = config.getString("chat.engagement.engage-path", "/v1/engage");
        this.connectTimeout = Duration.ofMillis(config.getLong("chat.engagement.connect-timeout-millis", 2000L));
        this.requestTimeout = Duration.ofMillis(config.getLong("chat.engagement.request-timeout-millis", 5000L));
        this.chatHistoryLimit = Math.max(1, config.getInt("chat.engagement.chat-history-limit", 10));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMinEmptyChatSeconds() {
        return minEmptyChatSeconds;
    }

    public int getMaxEmptyChatSeconds() {
        return maxEmptyChatSeconds;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getEngagePath() {
        return engagePath;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public int getChatHistoryLimit() {
        return chatHistoryLimit;
    }
}
