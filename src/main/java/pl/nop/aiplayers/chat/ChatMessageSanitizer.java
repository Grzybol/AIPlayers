package pl.nop.aiplayers.chat;

import java.util.regex.Pattern;

public final class ChatMessageSanitizer {

    private static final Pattern EMOJI_PATTERN = Pattern.compile("[\\p{So}\\p{Cs}]");

    private ChatMessageSanitizer() {
    }

    public static String sanitizeOutgoing(String message) {
        if (message == null) {
            return "";
        }
        String cleaned = stripSilenceTokens(message);
        cleaned = stripEmojis(cleaned);
        return cleaned.trim();
    }

    public static String stripSilenceTokens(String message) {
        if (message == null) {
            return "";
        }
        return message.replace("__SILENCE__", "").trim();
    }

    public static String stripEmojis(String message) {
        if (message == null) {
            return "";
        }
        return EMOJI_PATTERN.matcher(message).replaceAll("");
    }
}
