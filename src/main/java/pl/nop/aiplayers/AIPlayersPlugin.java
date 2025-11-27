package pl.nop.aiplayers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import pl.nop.aiplayers.ai.controller.AIControllerRegistry;
import pl.nop.aiplayers.ai.controller.DummyAIController;
import pl.nop.aiplayers.ai.controller.HttpAIController;
import pl.nop.aiplayers.ai.controller.OpenAIAIController;
import pl.nop.aiplayers.chat.AIChatListener;
import pl.nop.aiplayers.chat.AIChatService;
import pl.nop.aiplayers.command.AIPlayersCommand;
import pl.nop.aiplayers.command.AIPlayersTabCompleter;
import pl.nop.aiplayers.economy.AIEconomyService;
import pl.nop.aiplayers.manager.AIPlayerManager;
import pl.nop.aiplayers.storage.AIPlayerStorage;
import pl.nop.aiplayers.task.AITickTask;

public class AIPlayersPlugin extends JavaPlugin {

    private AIPlayerManager aiPlayerManager;
    private AIChatService chatService;
    private AIControllerRegistry controllerRegistry;
    private AIEconomyService economyService;
    private AIPlayerStorage storage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();

        this.chatService = new AIChatService(config.getInt("chat.history-size", 20));
        this.economyService = new AIEconomyService(this, config.getBoolean("economy.enabled", true));
        this.storage = new AIPlayerStorage(getDataFolder());

        this.aiPlayerManager = new AIPlayerManager(this, economyService);

        this.controllerRegistry = new AIControllerRegistry();
        this.controllerRegistry.registerDefaults(
                new DummyAIController(),
                new HttpAIController(this, config),
                new OpenAIAIController(this, config, new DummyAIController())
        );

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
        pluginManager.registerEvents(new AIChatListener(chatService), this);
    }

    private void startTickTask() {
        int interval = getConfig().getInt("ai.tick-interval-ticks", 10);
        new AITickTask(this, aiPlayerManager, controllerRegistry, economyService, chatService)
                .runTaskTimer(this, interval, interval);
    }

    @Override
    public void onDisable() {
        // TODO: persist AI players to disk using storage
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
}
