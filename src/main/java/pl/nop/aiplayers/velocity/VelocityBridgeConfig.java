package pl.nop.aiplayers.velocity;

import org.bukkit.configuration.file.FileConfiguration;

public class VelocityBridgeConfig {

    private final boolean enabled;
    private final String channel;
    private final int heartbeatSeconds;
    private final String serverId;
    private final int maxPlayersOverride;
    private final String authToken;

    public VelocityBridgeConfig(FileConfiguration config) {
        this.enabled = config.getBoolean("velocity.bridge.enabled", true);
        String configuredChannel = config.getString("velocity.bridge.channel", "aiplayers:count");
        if (configuredChannel == null || configuredChannel.isBlank()) {
            configuredChannel = "aiplayers:count";
        }
        this.channel = configuredChannel;
        this.heartbeatSeconds = Math.max(5, config.getInt("velocity.bridge.heartbeat-seconds", 10));
        String configuredServerId = config.getString("velocity.bridge.server-id", "");
        if (configuredServerId == null || configuredServerId.isBlank()) {
            configuredServerId = config.getString("ai.remote.server-id", "betterbox-1");
        }
        this.serverId = configuredServerId;
        this.maxPlayersOverride = Math.max(0, config.getInt("velocity.bridge.max-players-override", 0));
        String configuredAuthToken = config.getString("velocity.bridge.auth-token", "");
        if (configuredAuthToken == null) {
            configuredAuthToken = "";
        }
        this.authToken = configuredAuthToken;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getChannel() {
        return channel;
    }

    public int getHeartbeatSeconds() {
        return heartbeatSeconds;
    }

    public String getServerId() {
        return serverId;
    }

    public int getMaxPlayersOverride() {
        return maxPlayersOverride;
    }

    public String getAuthToken() {
        return authToken;
    }
}
