package pl.nop.aiplayers.velocity;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitTask;
import pl.nop.aiplayers.AIPlayersPlugin;
import pl.nop.aiplayers.logging.AIPlayersFileLogger;
import pl.nop.aiplayers.manager.AIPlayerManager;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

public class VelocityPlayerCountBridge {

    private static final String PROTOCOL = "aiplayers-count-v1";
    private final AIPlayersPlugin plugin;
    private final AIPlayerManager manager;
    private final VelocityBridgeConfig config;
    private final Gson gson;
    private final AtomicLong lastErrorLogAt;
    private BukkitTask heartbeatTask;
    private VelocityBridgePlayerListener listener;

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
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, config.getChannel());
        listener = new VelocityBridgePlayerListener(this);
        pluginManager.registerEvents(listener, plugin);
        logDebug("Registered outgoing plugin channel " + config.getChannel() + " and player listener.");
        scheduleHeartbeat();
        requestImmediateUpdate();
        logInfo("Velocity bridge enabled on channel " + config.getChannel() + ".");
        logDebug("Velocity bridge config: serverId=" + config.getServerId()
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
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, config.getChannel());
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
        Player player = getPlayerForMessaging();
        if (player == null) {
            logDebug("Skipping sendUpdate because no players are available for plugin messaging.");
            return;
        }
        int humans = manager.getOnlineHumansCount();
        int ai = manager.getOnlineAICount();
        int total = humans + ai;
        logDebug("Preparing payload: humans=" + humans + ", ai=" + ai + ", total=" + total);
        CountPayload payload = new CountPayload(
                config.getServerId(),
                System.currentTimeMillis(),
                humans,
                ai,
                total,
                config.getMaxPlayersOverride(),
                config.getAuthToken()
        );
        String json = gson.toJson(payload);
        logDebug("Serialized Velocity payload (" + json.length() + " bytes) for channel " + config.getChannel() + ".");
        try {
            player.sendPluginMessage(plugin, config.getChannel(), json.getBytes(StandardCharsets.UTF_8));
            logDebug("Velocity payload sent via player " + player.getName() + ".");
        } catch (Exception ex) {
            logFailure("Failed to send Velocity bridge update: " + ex.getMessage());
        }
    }

    private Player getPlayerForMessaging() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) {
            return null;
        }
        Player fallback = null;
        for (Player player : players) {
            if (fallback == null) {
                fallback = player;
            }
            if (manager.getSession(player.getName()) == null) {
                return player;
            }
        }
        return fallback;
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
        private final String protocol = PROTOCOL;
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

        private CountPayload(String serverId, long timestampMs, int onlineHumans, int onlineAi, int onlineTotal,
                             int maxPlayersOverride, String auth) {
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
