package pl.nop.aiplayers.velocity;

import org.bukkit.configuration.file.FileConfiguration;

public class VelocityBridgeConfig {

    private final boolean enabled;
    private final String socketPath;
    private final String protocol;
    private final int heartbeatSeconds;
    private final String serverId;
    private final int maxPlayersOverride;
    private final String authToken;

    public VelocityBridgeConfig(FileConfiguration config) {
        this.enabled = config.getBoolean("velocity.bridge.enabled", true);
        String configuredSocketPath = config.getString("velocity.bridge.socket-path", "/tmp/velocity-player-count-bridge.sock");
        if (configuredSocketPath == null || configuredSocketPath.isBlank()) {
            configuredSocketPath = "/tmp/velocity-player-count-bridge.sock";
        }
        this.socketPath = configuredSocketPath;
        String configuredProtocol = config.getString("velocity.bridge.protocol", "aiplayers-count-v1");
        if (configuredProtocol == null || configuredProtocol.isBlank()) {
            configuredProtocol = "aiplayers-count-v1";
        }
        this.protocol = configuredProtocol;
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

    public String getSocketPath() {
        return socketPath;
    }

    public String getProtocol() {
        return protocol;
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
