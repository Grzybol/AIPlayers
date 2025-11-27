package pl.nop.aiplayers.ai.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import pl.nop.aiplayers.ai.Action;
import pl.nop.aiplayers.ai.ActionType;
import pl.nop.aiplayers.ai.Perception;
import pl.nop.aiplayers.model.AIPlayerSession;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class HttpAIController implements AIController {

    private final Plugin plugin;
    private final Gson gson = new Gson();
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String baseUrl;
    private final int timeoutMillis;

    public HttpAIController(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.enabled = config.getBoolean("ai.http.enabled", false);
        this.baseUrl = config.getString("ai.http.base-url", "http://localhost:8081/ai/decision");
        this.timeoutMillis = config.getInt("ai.http.timeout-millis", 500);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMillis))
                .build();
    }

    @Override
    public CompletableFuture<Action> decide(AIPlayerSession session, Perception perception) {
        if (!enabled) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(() -> sendRequest(session, perception))
                .exceptionally(ex -> {
                    plugin.getLogger().warning("HTTP AI decide failed: " + ex.getMessage());
                    return null;
                });
    }

    private Action sendRequest(AIPlayerSession session, Perception perception) {
        try {
            JsonObject payload = buildPayload(perception);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .timeout(Duration.ofMillis(timeoutMillis))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                plugin.getLogger().warning("HTTP controller returned status " + response.statusCode());
                return null;
            }
            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            return parseAction(json, perception.getLocation());
        } catch (Exception ex) {
            plugin.getLogger().warning("HTTP AI error: " + ex.getMessage());
            return null;
        }
    }

    private JsonObject buildPayload(Perception perception) {
        JsonObject json = new JsonObject();
        json.addProperty("name", perception.getName());
        json.addProperty("uuid", perception.getUuid().toString());
        json.addProperty("world", perception.getWorld());
        json.addProperty("x", perception.getLocation().getX());
        json.addProperty("y", perception.getLocation().getY());
        json.addProperty("z", perception.getLocation().getZ());
        json.addProperty("serverTimeTicks", perception.getServerTimeTicks());
        // TODO: include chat and inventory snapshots
        return json;
    }

    private Action parseAction(JsonObject json, Location current) {
        if (json == null || !json.has("type")) {
            return null;
        }
        String typeString = json.get("type").getAsString();
        ActionType type;
        try {
            type = ActionType.valueOf(typeString.toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown action type from HTTP: " + typeString);
            return null;
        }
        switch (type) {
            case MOVE_TO:
                double x = json.has("x") ? json.get("x").getAsDouble() : current.getX();
                double y = json.has("y") ? json.get("y").getAsDouble() : current.getY();
                double z = json.has("z") ? json.get("z").getAsDouble() : current.getZ();
                return Action.moveTo(new Location(current.getWorld(), x, y, z));
            case SAY:
                return Action.say(json.has("message") ? json.get("message").getAsString() : "hi");
            case FOLLOW_PLAYER:
                return Action.follow(json.has("target") ? json.get("target").getAsString() : "");
            default:
                return Action.idle();
        }
    }
}
