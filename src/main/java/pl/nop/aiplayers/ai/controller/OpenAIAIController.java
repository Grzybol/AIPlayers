package pl.nop.aiplayers.ai.controller;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import pl.nop.aiplayers.ai.Action;
import pl.nop.aiplayers.ai.Perception;
import pl.nop.aiplayers.model.AIPlayerSession;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class OpenAIAIController implements AIController {

    private final Plugin plugin;
    private final boolean enabled;
    private final String apiKey;
    private final String model;
    private final AIController fallback;
    private final Logger logger;

    public OpenAIAIController(Plugin plugin, FileConfiguration config, AIController fallback) {
        this.plugin = plugin;
        this.enabled = config.getBoolean("ai.openai.enabled", false);
        this.apiKey = config.getString("ai.openai.api-key", "");
        this.model = config.getString("ai.openai.model", "gpt-4.1-mini");
        this.fallback = fallback;
        this.logger = plugin.getLogger();
    }

    @Override
    public CompletableFuture<Action> decide(AIPlayerSession session, Perception perception) {
        if (!enabled || apiKey.isEmpty()) {
            return fallback.decide(session, perception);
        }
        logger.info("OpenAI controller stub invoked for " + session.getProfile().getName() + " (model " + model + ")");
        // TODO: implement OpenAI call
        return fallback.decide(session, perception);
    }
}
