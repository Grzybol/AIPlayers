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
    private static final String DISCORD_API_BASE = "https://discord.com/api/v10";

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
        String guildId = config.getGuildId();
        String channelId = config.getChannelId();
        String botToken = config.getBotToken();
        if (guildId == null || guildId.isBlank()) {
            return;
        }
        if (channelId == null || channelId.isBlank()) {
            return;
        }
        if (botToken == null || botToken.isBlank()) {
            return;
        }
        if (message == null || message.isBlank()) {
            return;
        }
        String content = formatContent(botName, message);
        if (content.isBlank()) {
            return;
        }
        if (content.length() > DISCORD_MAX_MESSAGE_LENGTH) {
            content = content.substring(0, DISCORD_MAX_MESSAGE_LENGTH - 3) + "...";
        }
        DiscordPayload payload = new DiscordPayload(content);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> postMessage(payload));
    }

    private void postMessage(DiscordPayload payload) {
        String body = gson.toJson(payload);
        String channelId = config.getChannelId();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DISCORD_API_BASE + "/channels/" + channelId + "/messages"))
                .timeout(config.getRequestTimeout())
                .header("Content-Type", "application/json")
                .header("Authorization", "Bot " + config.getBotToken())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log("Discord API returned status " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception ex) {
            log("Discord API request failed: " + ex.getMessage());
        }
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
        private final Map<String, Object> allowed_mentions;

        private DiscordPayload(String content) {
            this.content = content;
            this.allowed_mentions = new HashMap<>();
            this.allowed_mentions.put("parse", new String[0]);
        }
    }
}
