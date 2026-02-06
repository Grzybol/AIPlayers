package pl.nop.aiplayers.chat.discord;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import pl.nop.aiplayers.logging.AIPlayersFileLogger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class DiscordWebhookRelay {
    private static final int DISCORD_MAX_MESSAGE_LENGTH = 2000;

    private final Plugin plugin;
    private final Gson gson;
    private final AIPlayersFileLogger fileLogger;
    private volatile DiscordRelayConfig config;
    private volatile HttpClient client;

    public DiscordWebhookRelay(Plugin plugin, DiscordRelayConfig config, AIPlayersFileLogger fileLogger) {
        this.plugin = plugin;
        this.gson = new Gson();
        this.fileLogger = fileLogger;
        updateConfig(config);
    }

    public void updateConfig(DiscordRelayConfig config) {
        this.config = config;
        this.client = HttpClient.newBuilder()
                .connectTimeout(config.getConnectTimeout())
                .build();
    }

    public void sendBotMessage(String botName, String message) {
        if (!config.isEnabled()) {
            return;
        }
        String webhookUrl = config.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        if (message == null || message.isBlank()) {
            return;
        }
        String username = resolveUsername(botName);
        String content = formatContent(botName, message);
        if (content.isBlank()) {
            return;
        }
        if (content.length() > DISCORD_MAX_MESSAGE_LENGTH) {
            content = content.substring(0, DISCORD_MAX_MESSAGE_LENGTH - 3) + "...";
        }
        String avatarUrl = config.getAvatarUrl();
        DiscordPayload payload = new DiscordPayload(content, username, avatarUrl);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> postWebhook(payload));
    }

    private void postWebhook(DiscordPayload payload) {
        String body = gson.toJson(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getWebhookUrl()))
                .timeout(config.getRequestTimeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log("Discord webhook returned status " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception ex) {
            log("Discord webhook failed: " + ex.getMessage());
        }
    }

    private String resolveUsername(String botName) {
        String configured = config.getUsername();
        if (configured == null || configured.isBlank()) {
            return botName;
        }
        return configured.replace("%bot%", botName == null ? "AIPlayer" : botName);
    }

    private String formatContent(String botName, String message) {
        String format = config.getMessageFormat();
        if (format == null || format.isBlank()) {
            format = "<%bot%> %message%";
        }
        String safeBot = botName == null ? "AIPlayer" : botName;
        String safeMessage = message == null ? "" : message;
        return format.replace("%bot%", safeBot).replace("%message%", safeMessage);
    }

    private void log(String message) {
        plugin.getLogger().warning(message);
        if (fileLogger != null) {
            fileLogger.warn(message);
        }
    }

    private static class DiscordPayload {
        private final String content;
        private final String username;
        private final String avatar_url;
        private final Map<String, Object> allowed_mentions;

        private DiscordPayload(String content, String username, String avatarUrl) {
            this.content = content;
            this.username = username;
            this.avatar_url = avatarUrl == null || avatarUrl.isBlank() ? null : avatarUrl;
            this.allowed_mentions = new HashMap<>();
            this.allowed_mentions.put("parse", new String[0]);
        }
    }
}
