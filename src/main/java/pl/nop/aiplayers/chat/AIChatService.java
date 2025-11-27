package pl.nop.aiplayers.chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import pl.nop.aiplayers.model.AIPlayerSession;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class AIChatService {

    private final Deque<String> chatHistory;
    private final int maxSize;

    public AIChatService(int maxSize) {
        this.chatHistory = new ArrayDeque<>();
        this.maxSize = maxSize;
    }

    public void recordMessage(String message) {
        if (chatHistory.size() >= maxSize) {
            chatHistory.removeFirst();
        }
        chatHistory.addLast(message);
    }

    public List<String> getChatHistorySnapshot() {
        return new ArrayList<>(chatHistory);
    }

    public void sendChatMessage(AIPlayerSession session, String message) {
        String formatted = ChatColor.GRAY + "<" + session.getProfile().getName() + "> " + ChatColor.WHITE + message;
        Bukkit.broadcastMessage(formatted);
        recordMessage(session.getProfile().getName() + ": " + message);
    }
}
