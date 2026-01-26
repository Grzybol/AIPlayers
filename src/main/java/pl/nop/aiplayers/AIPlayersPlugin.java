package pl.nop.aiplayers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import pl.nop.aiplayers.ai.controller.AIControllerRegistry;
import pl.nop.aiplayers.ai.controller.DummyAIController;
import pl.nop.aiplayers.ai.ActionExecutor;
import pl.nop.aiplayers.chat.AIChatListener;
import pl.nop.aiplayers.chat.AIChatService;
import pl.nop.aiplayers.command.AIPlayersCommand;
import pl.nop.aiplayers.command.AIPlayersTabCompleter;
import pl.nop.aiplayers.economy.AIEconomyService;
import pl.nop.aiplayers.manager.AIPlayerManager;
import pl.nop.aiplayers.model.AIBehaviorMode;
import pl.nop.aiplayers.model.AIControllerType;
import pl.nop.aiplayers.storage.AIPlayerStorage;
import pl.nop.aiplayers.task.AITickTask;

public class AIPlayersPlugin extends JavaPlugin {

    private AIPlayerManager aiPlayerManager;
    private AIChatService chatService;
    private AIControllerRegistry controllerRegistry;
    private AIEconomyService economyService;
    private AIPlayerStorage storage;
    private ActionExecutor actionExecutor;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        this.chatService = new AIChatService(this,
                config.getInt("chat.history-size", 20),
                config.getLong("chat.rate-limit-millis", 3000L));
        this.economyService = new AIEconomyService(this, config.getBoolean("economy.enabled", true));
        this.storage = new AIPlayerStorage(getDataFolder());

        AIControllerType defaultController = parseControllerType(config.getString("ai.default.controller-type", "DUMMY"));
        AIBehaviorMode defaultBehavior = parseBehaviorMode(config.getString("ai.default.behavior-mode", "WANDER"));
        this.aiPlayerManager = new AIPlayerManager(this, economyService, defaultController, defaultBehavior);

        this.actionExecutor = new ActionExecutor(chatService,
                config.getInt("ai.action-queue-size", 5),
                config.getLong("ai.action-timeout-millis", 4000L),
                config.getLong("ai.action-cooldown-millis", 500L));

        DummyAIController dummyController = new DummyAIController(config.getInt("chat.memory-size", 20));
        this.controllerRegistry = new AIControllerRegistry();
        this.controllerRegistry.registerDefaults(dummyController);

        loadProfiles();
        registerCommands();
        registerListeners();
        startTickTask();

        getLogger().info("AIPlayers enabled with tick interval " + config.getInt("ai.tick-interval-ticks", 10));
    }

    private void registerCommands() {
        AIPlayersCommand commandExecutor = new AIPlayersCommand(aiPlayerManager);
        getCommand("aiplayers").setExecutor(commandExecutor);
        getCommand("aiplayers").setTabCompleter(new AIPlayersTabCompleter(aiPlayerManager));
    }

    private void registerListeners() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new AIChatListener(this, chatService), this);
    }

    private void startTickTask() {
        int interval = getConfig().getInt("ai.tick-interval-ticks", 10);
        new AITickTask(this, aiPlayerManager, controllerRegistry, economyService, chatService, actionExecutor)
                .runTaskTimer(this, interval, interval);
    }

    @Override
    public void onDisable() {
        aiPlayerManager.getAllSessions().forEach(session ->
                session.getProfile().setLastKnownLocation(session.getNpcHandle().getLocation()));
        storage.saveAll(aiPlayerManager.getAllProfiles());
        aiPlayerManager.despawnAll();
        getLogger().info("AIPlayers disabled");
    }

    public AIPlayerManager getAiPlayerManager() {
        return aiPlayerManager;
    }

    public AIChatService getChatService() {
        return chatService;
    }

    public AIControllerRegistry getControllerRegistry() {
        return controllerRegistry;
    }

    public AIEconomyService getEconomyService() {
        return economyService;
    }

    public AIPlayerStorage getStorage() {
        return storage;
    }

    private void loadProfiles() {
        storage.loadAll().forEach(aiPlayerManager::addProfile);
        getLogger().info("Loaded " + aiPlayerManager.getAllProfiles().size() + " AI player profiles.");
    }

    private AIControllerType parseControllerType(String value) {
        if (value == null) {
            return AIControllerType.DUMMY;
        }
        try {
            return AIControllerType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            getLogger().warning("Unknown controller type in config: " + value + ", defaulting to DUMMY.");
            return AIControllerType.DUMMY;
        }
    }

    private AIBehaviorMode parseBehaviorMode(String value) {
        if (value == null) {
            return AIBehaviorMode.WANDER;
        }
        try {
            return AIBehaviorMode.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            getLogger().warning("Unknown behavior mode in config: " + value + ", defaulting to WANDER.");
            return AIBehaviorMode.WANDER;
        }
    }
}
