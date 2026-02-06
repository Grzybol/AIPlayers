package pl.nop.aiplayers.chat.discord;

import org.bukkit.configuration.Configuration;

import java.time.Duration;

public class DiscordRelayConfig {
    private final boolean enabled;
    private final String webhookUrl;
    private final String username;
    private final String avatarUrl;
    private final String messageFormat;
    private final Duration connectTimeout;
    private final Duration requestTimeout;

    public DiscordRelayConfig(Configuration config) {
        this.enabled = config.getBoolean("chat.discord.enabled", false);
        this.webhookUrl = config.getString("chat.discord.webhook-url", "");
        this.username = config.getString("chat.discord.username", "");
        this.avatarUrl = config.getString("chat.discord.avatar-url", "");
        this.messageFormat = config.getString("chat.discord.message-format", "<%bot%> %message%");
        long connectMillis = config.getLong("chat.discord.connect-timeout-millis", 2000L);
        long requestMillis = config.getLong("chat.discord.request-timeout-millis", 5000L);
        this.connectTimeout = Duration.ofMillis(Math.max(100L, connectMillis));
        this.requestTimeout = Duration.ofMillis(Math.max(500L, requestMillis));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getMessageFormat() {
        return messageFormat;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }
}
