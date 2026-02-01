package pl.nop.aiplayers.chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import pl.nop.aiplayers.AIPlayersPlugin;
import pl.nop.aiplayers.logging.AIPlayersFileLogger;
import pl.nop.aiplayers.model.AIPlayerSession;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class AIChatService {

    private final Plugin plugin;
    private final Deque<ChatEntry> chatHistory;
    private int maxSize;
    private long rateLimitMillis;
    private final Map<UUID, Long> lastMessageMillis = new ConcurrentHashMap<>();
    private final AtomicLong sequenceCounter = new AtomicLong(0L);
    private volatile long lastChatUpdateMillis;
    private volatile long lastPlayerChatUpdateMillis;
    private volatile long lastPlayerChatSequence;

    public AIChatService(Plugin plugin, int maxSize, long rateLimitMillis) {
        this.plugin = plugin;
        this.chatHistory = new ArrayDeque<>();
        this.maxSize = Math.max(1, maxSize);
        this.rateLimitMillis = Math.max(0, rateLimitMillis);
    }

    public synchronized void recordMessage(String sender, String message, ChatSenderType senderType) {
        if (chatHistory.size() >= maxSize) {
            chatHistory.removeFirst();
        }
        long now = System.currentTimeMillis();
        long sequence = sequenceCounter.incrementAndGet();
        chatHistory.addLast(ChatEntry.fromParts(sender, message, senderType, now, sequence));
        lastChatUpdateMillis = now;
        if (senderType == ChatSenderType.PLAYER) {
            lastPlayerChatUpdateMillis = now;
            lastPlayerChatSequence = sequence;
        }
        logToFile("Recorded chat message: " + sender + ": " + message);
    }

    public synchronized void recordMessage(String message) {
        if (message == null) {
            return;
        }
        ChatEntry entry = ChatEntry.fromLine(message, System.currentTimeMillis());
        recordMessage(entry.getSender(), entry.getMessage(), entry.getSenderType());
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

    public synchronized void updateSettings(int newMaxSize, long newRateLimitMillis) {
        this.maxSize = Math.max(1, newMaxSize);
        this.rateLimitMillis = Math.max(0, newRateLimitMillis);
        while (chatHistory.size() > maxSize) {
            chatHistory.removeFirst();
        }
    }

    public void sendChatMessage(AIPlayerSession session, String message) {
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(plugin, () -> sendChatMessage(session, message));
            return;
        }
        String sanitized = ChatMessageSanitizer.sanitizeOutgoing(message);
        if (sanitized.isBlank()) {
            return;
        }
        long now = System.currentTimeMillis();
        long last = lastMessageMillis.getOrDefault(session.getProfile().getUuid(), 0L);
        if (now - last < rateLimitMillis) {
            return;
        }
        lastMessageMillis.put(session.getProfile().getUuid(), now);
        String formatted = ChatColor.GRAY + "<" + session.getProfile().getName() + "> " + ChatColor.WHITE + sanitized;
        Bukkit.broadcastMessage(formatted);
        recordMessage(session.getProfile().getName(), sanitized, ChatSenderType.BOT);
        logToFile("AIPlayer " + session.getProfile().getName() + " sent chat message: " + sanitized);
    }

    public long getLastChatUpdateMillis() {
        return lastChatUpdateMillis;
    }

    public long getLastPlayerChatUpdateMillis() {
        return lastPlayerChatUpdateMillis;
    }

    public long getLastPlayerChatSequence() {
        return lastPlayerChatSequence;
    }

    private void logToFile(String message) {
        AIPlayersFileLogger fileLogger = getFileLogger();
        if (fileLogger != null) {
            fileLogger.info(message);
        }
    }

    private AIPlayersFileLogger getFileLogger() {
        if (plugin instanceof AIPlayersPlugin) {
            return ((AIPlayersPlugin) plugin).getFileLogger();
        }
        return null;
    }

    public static class ChatEntry {
        private final String rawLine;
        private final long timestampMillis;
        private final String sender;
        private final String message;
        private final ChatSenderType senderType;
        private final long sequence;

        private ChatEntry(String rawLine, long timestampMillis, String sender, String message, ChatSenderType senderType,
                          long sequence) {
            this.rawLine = rawLine;
            this.timestampMillis = timestampMillis;
            this.sender = sender;
            this.message = message;
            this.senderType = senderType;
            this.sequence = sequence;
        }

        public static ChatEntry fromParts(String sender, String message, ChatSenderType senderType,
                                          long timestampMillis, long sequence) {
            String safeSender = sender == null || sender.isBlank() ? "unknown" : sender.trim();
            String safeMessage = message == null ? "" : message.trim();
            String rawLine = safeSender + ": " + safeMessage;
            return new ChatEntry(rawLine, timestampMillis, safeSender, safeMessage, senderType, sequence);
        }

        public static ChatEntry fromLine(String rawLine, long timestampMillis) {
            if (rawLine == null) {
                return new ChatEntry("", timestampMillis, "unknown", "", ChatSenderType.UNKNOWN, 0L);
            }
            int idx = rawLine.indexOf(":");
            if (idx <= 0 || idx >= rawLine.length() - 1) {
                return new ChatEntry(rawLine, timestampMillis, "unknown", rawLine.trim(), ChatSenderType.UNKNOWN, 0L);
            }
            String sender = rawLine.substring(0, idx).trim();
            String message = rawLine.substring(idx + 1).trim();
            if (sender.isEmpty()) {
                sender = "unknown";
            }
            return new ChatEntry(rawLine, timestampMillis, sender, message, ChatSenderType.UNKNOWN, 0L);
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

        public ChatSenderType getSenderType() {
            return senderType;
        }

        public long getSequence() {
            return sequence;
        }
    }

    public enum ChatSenderType {
        PLAYER,
        BOT,
        UNKNOWN
    }
}
