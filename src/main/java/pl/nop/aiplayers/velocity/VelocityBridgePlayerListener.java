package pl.nop.aiplayers.velocity;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class VelocityBridgePlayerListener implements Listener {

    private final VelocityPlayerCountBridge bridge;

    public VelocityBridgePlayerListener(VelocityPlayerCountBridge bridge) {
        this.bridge = bridge;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        bridge.logDebug("Player join detected (" + event.getPlayer().getName() + "), triggering immediate update.");
        bridge.requestImmediateUpdate();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        bridge.logDebug("Player quit detected (" + event.getPlayer().getName() + "), triggering immediate update.");
        bridge.requestImmediateUpdate();
    }
}
