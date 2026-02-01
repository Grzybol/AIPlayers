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
        updatePingCount(event.getNumPlayers(), count -> event.setNumPlayers(count));
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        updatePingCount(event);
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

    private void updatePingCount(ServerListPingEvent event) {
        if (aiPlayerManager == null) {
            return;
        }
        int aiCount = aiPlayerManager.getOnlineSessionCount();
        if (aiCount <= 0) {
            return;
        }
        int currentPlayers = getNumPlayers(event);
        int updatedPlayers = currentPlayers + aiCount;
        if (!setNumPlayers(event, updatedPlayers)) {
            event.setMaxPlayers(Math.max(event.getMaxPlayers(), updatedPlayers));
        }
    }

    private int getNumPlayers(ServerListPingEvent event) {
        try {
            Method method = event.getClass().getMethod("getNumPlayers");
            Object value = method.invoke(event);
            if (value instanceof Integer) {
                return (Integer) value;
            }
        } catch (ReflectiveOperationException ignored) {
            // Ignore and fall back to 0.
        }
        return 0;
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
