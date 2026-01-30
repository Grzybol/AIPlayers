package pl.nop.aiplayers.chat;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

public class AIChatListener implements Listener {

    private final AIChatService chatService;
    private final Plugin plugin;

    public AIChatListener(Plugin plugin, AIChatService chatService) {
        this.plugin = plugin;
        this.chatService = chatService;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        scheduler.runTask(plugin, () -> chatService.recordMessage(event.getPlayer().getName() + ": " + event.getMessage()));
    }
}
