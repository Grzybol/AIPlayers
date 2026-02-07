package pl.nop.aiplayers.chat.engagement;

import org.bukkit.configuration.file.FileConfiguration;

import java.time.Duration;
import java.util.List;

public class ChatEngagementConfig {

    private final boolean enabled;
    private final boolean bot2BotEnabled;
    private final int minEmptyChatSeconds;
    private final int maxEmptyChatSeconds;
    private final int minBot2BotEmptyChatSeconds;
    private final int maxBot2BotEmptyChatSeconds;
    private final String baseUrl;
    private final String engagementPath;
    private final Duration connectTimeout;
    private final Duration requestTimeout;
    private final int chatHistoryLimit;
    private final int plannerMaxActions;
    private final int plannerMinDelayMs;
    private final int plannerMaxDelayMs;
    private final double plannerGlobalSilenceChance;
    private final double plannerReplyChance;
    private final double bot2BotBaseSilenceChance;
    private final double bot2BotSilenceMultiplier;
    private final String personaLanguage;
    private final String personaTone;
    private final String personaKnowledgeLevel;
    private final List<String> personaStyleTags;
    private final List<String> personaAvoidTopics;
    private final String serverId;
    private final String serverMode;

    public ChatEngagementConfig(FileConfiguration config) {
        this.enabled = config.getBoolean("chat.engagement.engage-players-on-chat", false);
        int minSeconds = Math.max(1, config.getInt("chat.engagement.min-empty-chat-time-to-engage-in-seconds", 120));
        int maxSeconds = Math.max(minSeconds, config.getInt("chat.engagement.max-empty-chat-time-to-engage-in-seconds", 300));
        this.minEmptyChatSeconds = minSeconds;
        this.maxEmptyChatSeconds = maxSeconds;
        this.bot2BotEnabled = config.getBoolean("chat.engagement.bot2bot.enabled", false);
        int minBotSeconds = Math.max(1, config.getInt("chat.engagement.bot2bot.min-empty-chat-time-to-engage-in-seconds", 180));
        int maxBotSeconds = Math.max(minBotSeconds,
                config.getInt("chat.engagement.bot2bot.max-empty-chat-time-to-engage-in-seconds", 360));
        this.minBot2BotEmptyChatSeconds = minBotSeconds;
        this.maxBot2BotEmptyChatSeconds = maxBotSeconds;
        this.baseUrl = config.getString("chat.engagement.base-url", "");
        String configuredEngagementPath = config.getString("chat.engagement.engagement-path");
        if (configuredEngagementPath == null || configuredEngagementPath.isBlank()) {
            configuredEngagementPath = config.getString("chat.engagement.plan-path", "/v1/plan");
        }
        this.engagementPath = configuredEngagementPath;
        this.connectTimeout = Duration.ofMillis(config.getLong("chat.engagement.connect-timeout-millis", 2000L));
        this.requestTimeout = Duration.ofMillis(config.getLong("chat.engagement.request-timeout-millis", 5000L));
        this.chatHistoryLimit = Math.max(1, config.getInt("chat.engagement.chat-history-limit", 10));
        this.plannerMaxActions = Math.max(1, config.getInt("ai.remote.settings.max-actions", 3));
        this.plannerMinDelayMs = Math.max(0, config.getInt("ai.remote.settings.min-delay-ms", 800));
        this.plannerMaxDelayMs = Math.max(plannerMinDelayMs, config.getInt("ai.remote.settings.max-delay-ms", 4500));
        this.plannerGlobalSilenceChance = clampChance(
                config.getDouble("ai.remote.settings.global-silence-chance", 0.25));
        this.plannerReplyChance = clampChance(config.getDouble("ai.remote.settings.reply-chance", 0.65));
        this.bot2BotBaseSilenceChance = clampChance(
                config.getDouble("chat.engagement.bot2bot.base-silence-chance", 0.3));
        this.bot2BotSilenceMultiplier = Math.max(0.0,
                config.getDouble("chat.engagement.bot2bot.silence-multiplier", 0.5));
        this.personaLanguage = config.getString("ai.remote.persona.language", "pl");
        this.personaTone = config.getString("ai.remote.persona.tone", "casual");
        this.personaKnowledgeLevel = config.getString("ai.remote.persona.knowledge-level", "average_player");
        this.personaStyleTags = config.getStringList("ai.remote.persona.style-tags");
        this.personaAvoidTopics = config.getStringList("ai.remote.persona.avoid-topics");
        this.serverId = config.getString("ai.remote.server-id", "betterbox-1");
        this.serverMode = config.getString("ai.remote.server-mode", "LOBBY");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isBot2BotEnabled() {
        return bot2BotEnabled;
    }

    public int getMinEmptyChatSeconds() {
        return minEmptyChatSeconds;
    }

    public int getMaxEmptyChatSeconds() {
        return maxEmptyChatSeconds;
    }

    public int getMinBot2BotEmptyChatSeconds() {
        return minBot2BotEmptyChatSeconds;
    }

    public int getMaxBot2BotEmptyChatSeconds() {
        return maxBot2BotEmptyChatSeconds;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getEngagementPath() {
        return engagementPath;
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

    public int getPlannerMaxActions() {
        return plannerMaxActions;
    }

    public int getPlannerMinDelayMs() {
        return plannerMinDelayMs;
    }

    public int getPlannerMaxDelayMs() {
        return plannerMaxDelayMs;
    }

    public double getPlannerGlobalSilenceChance() {
        return plannerGlobalSilenceChance;
    }

    public double getPlannerReplyChance() {
        return plannerReplyChance;
    }

    public double getBot2BotBaseSilenceChance() {
        return bot2BotBaseSilenceChance;
    }

    public double getBot2BotSilenceMultiplier() {
        return bot2BotSilenceMultiplier;
    }

    public String getPersonaLanguage() {
        return personaLanguage;
    }

    public String getPersonaTone() {
        return personaTone;
    }

    public String getPersonaKnowledgeLevel() {
        return personaKnowledgeLevel;
    }

    public List<String> getPersonaStyleTags() {
        return personaStyleTags;
    }

    public List<String> getPersonaAvoidTopics() {
        return personaAvoidTopics;
    }

    public String getServerId() {
        return serverId;
    }

    public String getServerMode() {
        return serverMode;
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
}
