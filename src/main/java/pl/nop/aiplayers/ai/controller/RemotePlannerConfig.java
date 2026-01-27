package pl.nop.aiplayers.ai.controller;

import org.bukkit.configuration.file.FileConfiguration;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class RemotePlannerConfig {

    private final boolean enabled;
    private final String baseUrl;
    private final String planPath;
    private final Duration requestTimeout;
    private final String serverId;
    private final String serverMode;
    private final PlannerSettings settings;
    private final int chatLimit;
    private final long requestIntervalMillis;
    private final String personaLanguage;
    private final String personaTone;
    private final List<String> personaStyleTags;
    private final List<String> personaAvoidTopics;
    private final String personaKnowledgeLevel;

    public RemotePlannerConfig(FileConfiguration config) {
        this.enabled = config.getBoolean("ai.remote.enabled", false);
        this.baseUrl = config.getString("ai.remote.base-url", "");
        this.planPath = config.getString("ai.remote.plan-path", "/v1/plan");
        this.requestTimeout = Duration.ofMillis(config.getLong("ai.remote.request-timeout-millis", 1500L));
        this.serverId = config.getString("ai.remote.server-id", "betterbox-1");
        this.serverMode = config.getString("ai.remote.server-mode", "LOBBY");
        this.chatLimit = Math.max(1, config.getInt("ai.remote.chat-limit", 10));
        this.requestIntervalMillis = Math.max(30000L, config.getLong("ai.remote.request-interval-millis", 30000L));
        this.settings = new PlannerSettings(
                config.getInt("ai.remote.settings.max-actions", 3),
                config.getInt("ai.remote.settings.min-delay-ms", 800),
                config.getInt("ai.remote.settings.max-delay-ms", 4500),
                config.getDouble("ai.remote.settings.global-silence-chance", 0.25),
                config.getDouble("ai.remote.settings.reply-chance", 0.65)
        );
        this.personaLanguage = config.getString("ai.remote.persona.language", "pl");
        this.personaTone = config.getString("ai.remote.persona.tone", "casual");
        this.personaStyleTags = listOrEmpty(config.getStringList("ai.remote.persona.style-tags"));
        this.personaAvoidTopics = listOrEmpty(config.getStringList("ai.remote.persona.avoid-topics"));
        this.personaKnowledgeLevel = config.getString("ai.remote.persona.knowledge-level", "average_player");
    }

    private List<String> listOrEmpty(List<String> items) {
        if (items == null) {
            return Collections.emptyList();
        }
        return items;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getPlanPath() {
        return planPath;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public String getServerId() {
        return serverId;
    }

    public String getServerMode() {
        return serverMode;
    }

    public PlannerSettings getSettings() {
        return settings;
    }

    public int getChatLimit() {
        return chatLimit;
    }

    public long getRequestIntervalMillis() {
        return requestIntervalMillis;
    }

    public String getPersonaLanguage() {
        return personaLanguage;
    }

    public String getPersonaTone() {
        return personaTone;
    }

    public List<String> getPersonaStyleTags() {
        return personaStyleTags;
    }

    public List<String> getPersonaAvoidTopics() {
        return personaAvoidTopics;
    }

    public String getPersonaKnowledgeLevel() {
        return personaKnowledgeLevel;
    }
}
