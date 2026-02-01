package pl.nop.aiplayers.chat;

import java.util.regex.Pattern;

public final class ChatMessageSanitizer {

    private static final Pattern EMOJI_PATTERN = Pattern.compile("[\\p{So}\\p{Cs}]");
    private static final Pattern SYSTEM_TAG_PATTERN = Pattern.compile("(?i)===\\s*[^=]+\\s*===");
    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile("```\\s*\\w*");

    private ChatMessageSanitizer() {
    }

    public static String sanitizeOutgoing(String message) {
        if (message == null) {
            return "";
        }
        if (containsSystemTags(message)) {
            return "";
        }
        String cleaned = stripSilenceTokens(message);
        cleaned = stripCodeFences(cleaned);
        if (containsSystemTags(cleaned)) {
            return "";
        }
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

    private static boolean containsSystemTags(String message) {
        if (message == null) {
            return false;
        }
        return SYSTEM_TAG_PATTERN.matcher(message).find();
    }

    private static String stripCodeFences(String message) {
        if (message == null) {
            return "";
        }
        String cleaned = CODE_FENCE_PATTERN.matcher(message).replaceAll("");
        cleaned = cleaned.replace("```", "");
        return cleaned;
    }
}
