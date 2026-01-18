package pl.nop.aiplayers.chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import pl.nop.aiplayers.model.AIPlayerSession;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AIChatService {

    private final Plugin plugin;
    private final Deque<String> chatHistory;
    private final int maxSize;
    private final long rateLimitMillis;
    private final Map<UUID, Long> lastMessageMillis = new ConcurrentHashMap<>();

    public AIChatService(Plugin plugin, int maxSize, long rateLimitMillis) {
        this.plugin = plugin;
        this.chatHistory = new ArrayDeque<>();
        this.maxSize = maxSize;
        this.rateLimitMillis = rateLimitMillis;
    }

    public synchronized void recordMessage(String message) {
        if (chatHistory.size() >= maxSize) {
            chatHistory.removeFirst();
        }
        chatHistory.addLast(message);
    }

    public synchronized List<String> getChatHistorySnapshot() {
        return new ArrayList<>(chatHistory);
    }

    public void sendChatMessage(AIPlayerSession session, String message) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> sendChatMessage(session, message));
            return;
        }
        if (message == null || message.isBlank()) {
            return;
        }
        long now = System.currentTimeMillis();
        long last = lastMessageMillis.getOrDefault(session.getProfile().getUuid(), 0L);
        if (now - last < rateLimitMillis) {
            return;
        }
        lastMessageMillis.put(session.getProfile().getUuid(), now);
        String formatted = ChatColor.GRAY + "<" + session.getProfile().getName() + "> " + ChatColor.WHITE + message;
        Bukkit.broadcastMessage(formatted);
        recordMessage(session.getProfile().getName() + ": " + message);
    }
}
