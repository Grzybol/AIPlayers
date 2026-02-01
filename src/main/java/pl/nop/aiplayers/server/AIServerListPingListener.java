package pl.nop.aiplayers.server;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import pl.nop.aiplayers.manager.AIPlayerManager;

public class AIServerListPingListener implements Listener {

    private final AIPlayerManager aiPlayerManager;

    public AIServerListPingListener(AIPlayerManager aiPlayerManager) {
        this.aiPlayerManager = aiPlayerManager;
    }

    @EventHandler
    public void onServerListPing(PaperServerListPingEvent event) {
        updatePingCount(event.getNumPlayers(), count -> event.setNumPlayers(count));
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        updatePingCount(event.getNumPlayers(), count -> event.setNumPlayers(count));
    }

    private void updatePingCount(int currentPlayers, java.util.function.IntConsumer updateFn) {
        if (aiPlayerManager == null) {
            return;
        }
        int aiCount = aiPlayerManager.getOnlineSessionCount();
        if (aiCount <= 0) {
            return;
        }
        updateFn.accept(currentPlayers + aiCount);
    }
}
