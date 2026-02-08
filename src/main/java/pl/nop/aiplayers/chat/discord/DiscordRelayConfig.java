package pl.nop.aiplayers.chat.discord;

import org.bukkit.configuration.Configuration;

import java.time.Duration;

public class DiscordRelayConfig {
    private final boolean enabled;
    private final String guildId;
    private final String channelId;
    private final String botToken;
    private final String messageFormat;
    private final Duration connectTimeout;
    private final Duration requestTimeout;

    public DiscordRelayConfig(Configuration config) {
        this.enabled = config.getBoolean("chat.discord.enabled", false);
        this.guildId = config.getString("chat.discord.guild-id", "");
        this.channelId = config.getString("chat.discord.channel-id", "");
        this.botToken = config.getString("chat.discord.bot-token", "");
        this.messageFormat = config.getString("chat.discord.message-format", "<%bot%> %message%");
        long connectMillis = config.getLong("chat.discord.connect-timeout-millis", 2000L);
        long requestMillis = config.getLong("chat.discord.request-timeout-millis", 5000L);
        this.connectTimeout = Duration.ofMillis(Math.max(100L, connectMillis));
        this.requestTimeout = Duration.ofMillis(Math.max(500L, requestMillis));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getBotToken() {
        return botToken;
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
