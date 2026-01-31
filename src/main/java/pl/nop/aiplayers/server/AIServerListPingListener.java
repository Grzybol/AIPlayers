package pl.nop.aiplayers.server;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import pl.nop.aiplayers.manager.AIPlayerManager;

public class AIServerListPingListener implements Listener {

    private final AIPlayerManager aiPlayerManager;

    public AIServerListPingListener(AIPlayerManager aiPlayerManager) {
        this.aiPlayerManager = aiPlayerManager;
    }

    @EventHandler
    public void onServerListPing(PaperServerListPingEvent event) {
        if (aiPlayerManager == null) {
            return;
        }
        int aiCount = aiPlayerManager.getAllSessions().size();
        if (aiCount <= 0) {
            return;
        }
        event.setNumPlayers(event.getNumPlayers() + aiCount);
    }
}
