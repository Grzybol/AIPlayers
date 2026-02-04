package pl.nop.aiplayers.velocity;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import pl.nop.aiplayers.AIPlayersPlugin;
import pl.nop.aiplayers.logging.AIPlayersFileLogger;
import pl.nop.aiplayers.manager.AIPlayerManager;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

public class VelocityPlayerCountBridge {

    private final AIPlayersPlugin plugin;
    private final AIPlayerManager manager;
    private final VelocityBridgeConfig config;
    private final Gson gson;
    private final AtomicLong lastErrorLogAt;
    private BukkitTask heartbeatTask;

    public VelocityPlayerCountBridge(AIPlayersPlugin plugin, AIPlayerManager manager, VelocityBridgeConfig config) {
        this.plugin = plugin;
        this.manager = manager;
        this.config = config;
        this.gson = new Gson();
        this.lastErrorLogAt = new AtomicLong(0L);
    }

    public void start() {
        if (!config.isEnabled()) {
            logDebug("Velocity bridge start skipped (disabled).");
            return;
        }
        scheduleHeartbeat();
        requestImmediateUpdate();
        logInfo("Velocity bridge enabled on unix socket " + config.getSocketPath() + ".");
        logDebug("Velocity bridge config: serverId=" + config.getServerId()
                + ", protocol=" + config.getProtocol()
                + ", socketPath=" + config.getSocketPath()
                + ", heartbeatSeconds=" + config.getHeartbeatSeconds()
                + ", maxPlayersOverride=" + config.getMaxPlayersOverride()
                + ", authTokenPresent=" + (!config.getAuthToken().isBlank()));
    }

    public void shutdown() {
        if (!config.isEnabled()) {
            logDebug("Velocity bridge shutdown skipped (disabled).");
            return;
        }
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
            heartbeatTask = null;
        }
        logDebug("Velocity bridge shutdown complete.");
    }

    public void requestImmediateUpdate() {
        if (!config.isEnabled()) {
            logDebug("Immediate update requested but bridge disabled.");
            return;
        }
        logDebug("Immediate update requested; scheduling send on main thread.");
        Bukkit.getScheduler().runTask(plugin, this::sendUpdate);
    }

    private void scheduleHeartbeat() {
        int intervalTicks = config.getHeartbeatSeconds() * 20;
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
        }
        heartbeatTask = Bukkit.getScheduler().runTaskTimer(plugin, this::sendUpdate, intervalTicks, intervalTicks);
        logDebug("Heartbeat scheduled every " + config.getHeartbeatSeconds() + "s (" + intervalTicks + " ticks).");
    }

    private void sendUpdate() {
        if (!config.isEnabled()) {
            logDebug("Skipping sendUpdate because bridge disabled.");
            return;
        }
        int humans = manager.getOnlineHumansCount();
        int ai = manager.getLoadedBotCount();
        int total = humans + ai;
        logDebug("Preparing payload: humans=" + humans + ", ai=" + ai + ", total=" + total);
        CountPayload payload = new CountPayload(
                config.getProtocol(),
                config.getServerId(),
                System.currentTimeMillis(),
                humans,
                ai,
                total,
                config.getMaxPlayersOverride(),
                config.getAuthToken()
        );
        String json = gson.toJson(payload);
        logDebug("Serialized Velocity payload (" + json.length() + " bytes) for unix socket " + config.getSocketPath() + ".");
        try {
            sendPayload(json);
        } catch (Exception ex) {
            logFailure("Failed to send Velocity bridge update: " + ex.getMessage());
        }
    }

    private void sendPayload(String json) throws IOException {
        Path socketPath = Path.of(config.getSocketPath());
        UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketPath);
        try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
            channel.connect(address);
            ByteBuffer buffer = ByteBuffer.wrap((json + "\n").getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
            logDebug("Velocity payload sent via unix socket " + config.getSocketPath() + ".");
        }
    }

    private void logFailure(String message) {
        long now = System.currentTimeMillis();
        long last = lastErrorLogAt.get();
        if (now - last > 30000L && lastErrorLogAt.compareAndSet(last, now)) {
            plugin.getLogger().warning(message);
            AIPlayersFileLogger fileLogger = plugin.getFileLogger();
            if (fileLogger != null) {
                fileLogger.warn(message);
            }
        }
    }

    private void logInfo(String message) {
        plugin.getLogger().info(message);
        AIPlayersFileLogger fileLogger = plugin.getFileLogger();
        if (fileLogger != null) {
            fileLogger.info(message);
        }
    }

    void logDebug(String message) {
        String decorated = "[VelocityBridge][debug] " + message;
        plugin.getLogger().info(decorated);
        AIPlayersFileLogger fileLogger = plugin.getFileLogger();
        if (fileLogger != null) {
            fileLogger.info(decorated);
        }
    }

    private static class CountPayload {
        private final String protocol;
        @SerializedName("server_id")
        private final String serverId;
        @SerializedName("timestamp_ms")
        private final long timestampMs;
        @SerializedName("online_humans")
        private final int onlineHumans;
        @SerializedName("online_ai")
        private final int onlineAi;
        @SerializedName("online_total")
        private final int onlineTotal;
        @SerializedName("max_players_override")
        private final int maxPlayersOverride;
        @SerializedName("auth")
        private final String auth;

        private CountPayload(String protocol, String serverId, long timestampMs, int onlineHumans, int onlineAi, int onlineTotal,
                             int maxPlayersOverride, String auth) {
            this.protocol = protocol;
            this.serverId = serverId;
            this.timestampMs = timestampMs;
            this.onlineHumans = onlineHumans;
            this.onlineAi = onlineAi;
            this.onlineTotal = onlineTotal;
            this.maxPlayersOverride = maxPlayersOverride;
            this.auth = auth;
        }
    }
}
