package pl.nop.aiplayers.chat;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class AIChatListener implements Listener {

    private final AIChatService chatService;

    public AIChatListener(AIChatService chatService) {
        this.chatService = chatService;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        chatService.recordMessage(event.getPlayer().getName() + ": " + event.getMessage());
    }
}
