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
            return;
        }
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, config.getChannel());
        listener = new VelocityBridgePlayerListener(this);
        pluginManager.registerEvents(listener, plugin);
        scheduleHeartbeat();
        requestImmediateUpdate();
        logInfo("Velocity bridge enabled on channel " + config.getChannel() + ".");
    }

    public void shutdown() {
        if (!config.isEnabled()) {
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
    }

    public void requestImmediateUpdate() {
        if (!config.isEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, this::sendUpdate);
    }

    private void scheduleHeartbeat() {
        int intervalTicks = config.getHeartbeatSeconds() * 20;
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
        }
        heartbeatTask = Bukkit.getScheduler().runTaskTimer(plugin, this::sendUpdate, intervalTicks, intervalTicks);
    }

    private void sendUpdate() {
        if (!config.isEnabled()) {
            return;
        }
        Player player = getAnyPlayer();
        if (player == null) {
            return;
        }
        int humans = manager.getOnlineHumansCount();
        int ai = manager.getOnlineAICount();
        int total = humans + ai;
        CountPayload payload = new CountPayload(
                config.getServerId(),
                System.currentTimeMillis(),
                humans,
                ai,
                total,
                config.getMaxPlayersOverride()
        );
        String json = gson.toJson(payload);
        try {
            player.sendPluginMessage(plugin, config.getChannel(), json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            logFailure("Failed to send Velocity bridge update: " + ex.getMessage());
        }
    }

    private Player getAnyPlayer() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (players.isEmpty()) {
            return null;
        }
        return players.iterator().next();
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

        private CountPayload(String serverId, long timestampMs, int onlineHumans, int onlineAi, int onlineTotal,
                             int maxPlayersOverride) {
            this.serverId = serverId;
            this.timestampMs = timestampMs;
            this.onlineHumans = onlineHumans;
            this.onlineAi = onlineAi;
            this.onlineTotal = onlineTotal;
            this.maxPlayersOverride = maxPlayersOverride;
        }
    }
}
