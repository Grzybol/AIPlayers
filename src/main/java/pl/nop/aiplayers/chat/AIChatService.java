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
    private final Deque<ChatEntry> chatHistory;
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
        chatHistory.addLast(ChatEntry.fromLine(message, System.currentTimeMillis()));
    }

    public synchronized List<String> getChatHistorySnapshot() {
        List<String> history = new ArrayList<>();
        for (ChatEntry entry : chatHistory) {
            history.add(entry.getRawLine());
        }
        return history;
    }

    public synchronized List<ChatEntry> getChatEntriesSnapshot() {
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

    public static class ChatEntry {
        private final String rawLine;
        private final long timestampMillis;
        private final String sender;
        private final String message;

        private ChatEntry(String rawLine, long timestampMillis, String sender, String message) {
            this.rawLine = rawLine;
            this.timestampMillis = timestampMillis;
            this.sender = sender;
            this.message = message;
        }

        public static ChatEntry fromLine(String rawLine, long timestampMillis) {
            if (rawLine == null) {
                return new ChatEntry("", timestampMillis, "unknown", "");
            }
            int idx = rawLine.indexOf(":");
            if (idx <= 0 || idx >= rawLine.length() - 1) {
                return new ChatEntry(rawLine, timestampMillis, "unknown", rawLine.trim());
            }
            String sender = rawLine.substring(0, idx).trim();
            String message = rawLine.substring(idx + 1).trim();
            if (sender.isEmpty()) {
                sender = "unknown";
            }
            return new ChatEntry(rawLine, timestampMillis, sender, message);
        }

        public String getRawLine() {
            return rawLine;
        }

        public long getTimestampMillis() {
            return timestampMillis;
        }

        public String getSender() {
            return sender;
        }

        public String getMessage() {
            return message;
        }
    }
}
