package pl.nop.aiplayers.server;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import java.lang.reflect.Method;
import org.bukkit.event.server.ServerListPingEvent;
import pl.nop.aiplayers.manager.AIPlayerManager;

public class AIServerListPingListener implements Listener {

    private final AIPlayerManager aiPlayerManager;

    public AIServerListPingListener(AIPlayerManager aiPlayerManager) {
        this.aiPlayerManager = aiPlayerManager;
    }

    @EventHandler
    public void onServerListPing(PaperServerListPingEvent event) {
        updatePingCount(count -> event.setNumPlayers(count), event::getMaxPlayers, event::setMaxPlayers);
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        updatePingCount(event);
    }

    private void updatePingCount(java.util.function.IntConsumer updateFn,
                                 java.util.function.IntSupplier maxSupplier,
                                 java.util.function.IntConsumer maxUpdater) {
        if (aiPlayerManager == null) {
            return;
        }
        int totalPlayers = aiPlayerManager.getReportedPlayerCount();
        updateFn.accept(totalPlayers);
        if (maxSupplier != null && maxUpdater != null && totalPlayers > maxSupplier.getAsInt()) {
            maxUpdater.accept(totalPlayers);
        }
    }

    private void updatePingCount(ServerListPingEvent event) {
        if (aiPlayerManager == null) {
            return;
        }
        int totalPlayers = aiPlayerManager.getReportedPlayerCount();
        if (!setNumPlayers(event, totalPlayers)) {
            event.setMaxPlayers(Math.max(event.getMaxPlayers(), totalPlayers));
            return;
        }
        if (totalPlayers > event.getMaxPlayers()) {
            event.setMaxPlayers(totalPlayers);
        }
    }

    private boolean setNumPlayers(ServerListPingEvent event, int count) {
        try {
            Method method = event.getClass().getMethod("setNumPlayers", int.class);
            method.invoke(event, count);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
