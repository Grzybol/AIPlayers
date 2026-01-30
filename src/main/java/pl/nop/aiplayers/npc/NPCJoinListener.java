package pl.nop.aiplayers.npc;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import pl.nop.aiplayers.manager.AIPlayerManager;
import pl.nop.aiplayers.model.AIPlayerSession;

public class NPCJoinListener implements Listener {

    private final Plugin plugin;
    private final AIPlayerManager manager;

    public NPCJoinListener(Plugin plugin, AIPlayerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (AIPlayerSession session : manager.getAllSessions()) {
                NPCHandle npc = session.getNpcHandle();
                if (npc.getLocation() == null) {
                    continue;
                }
                npc.showTo(player);
            }
        });
    }
}
