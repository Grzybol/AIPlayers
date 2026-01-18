package pl.nop.aiplayers.task;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.nop.aiplayers.ai.Action;
import pl.nop.aiplayers.ai.ActionExecutor;
import pl.nop.aiplayers.ai.Perception;
import pl.nop.aiplayers.ai.controller.AIController;
import pl.nop.aiplayers.ai.controller.AIControllerRegistry;
import pl.nop.aiplayers.chat.AIChatService;
import pl.nop.aiplayers.economy.AIEconomyService;
import pl.nop.aiplayers.manager.AIPlayerManager;
import pl.nop.aiplayers.model.AIPlayerSession;
import pl.nop.aiplayers.npc.NPCHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AITickTask extends BukkitRunnable {

    private final pl.nop.aiplayers.AIPlayersPlugin plugin;
    private final AIPlayerManager manager;
    private final AIControllerRegistry controllerRegistry;
    private final AIEconomyService economyService;
    private final AIChatService chatService;
    private final ActionExecutor actionExecutor;

    public AITickTask(pl.nop.aiplayers.AIPlayersPlugin plugin, AIPlayerManager manager, AIControllerRegistry controllerRegistry,
                      AIEconomyService economyService, AIChatService chatService, ActionExecutor actionExecutor) {
        this.plugin = plugin;
        this.manager = manager;
        this.controllerRegistry = controllerRegistry;
        this.economyService = economyService;
        this.chatService = chatService;
        this.actionExecutor = actionExecutor;
    }

    @Override
    public void run() {
        for (AIPlayerSession session : manager.getAllSessions()) {
            NPCHandle npc = session.getNpcHandle();
            if (npc.getLocation() == null) {
                continue;
            }
            actionExecutor.tick(session);
            Perception perception = buildPerception(session);
            AIController controller = controllerRegistry.getController(session.getProfile().getControllerType());
            CompletableFuture<Action> future = controller.decide(session, perception);
            future.thenAccept(action -> {
                if (action == null) {
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () -> actionExecutor.submit(session, action));
            });
        }
    }

    private Perception buildPerception(AIPlayerSession session) {
        NPCHandle npc = session.getNpcHandle();
        Location loc = npc.getLocation();
        List<String> nearbyPlayers = new ArrayList<>();
        for (Player player : loc.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(loc) < 225) {
                nearbyPlayers.add(player.getName() + ":" + Math.sqrt(player.getLocation().distanceSquared(loc)));
            }
        }
        List<String> nearbyAi = new ArrayList<>();
        for (AIPlayerSession other : manager.getAllSessions()) {
            if (other == session) {
                continue;
            }
            Location otherLoc = other.getNpcHandle().getLocation();
            if (otherLoc != null && otherLoc.getWorld().equals(loc.getWorld()) && otherLoc.distanceSquared(loc) < 225) {
                nearbyAi.add(other.getProfile().getName());
            }
        }
        double balance = economyService.getBalance(session.getProfile());
        List<String> inventory = summarizeInventory(session);
        List<String> chat = chatService.getChatHistorySnapshot();
        return new Perception(session.getProfile().getName(), session.getProfile().getUuid(), loc.getWorld().getName(), loc.clone(),
                nearbyPlayers, nearbyAi, balance, inventory, chat, plugin.getServer().getCurrentTick());
    }

    private List<String> summarizeInventory(AIPlayerSession session) {
        List<String> summary = new ArrayList<>();
        session.getInventory().forEach(item -> {
            if (item != null) {
                summary.add(item.getType().name() + " x" + item.getAmount());
            }
        });
        return summary;
    }

    // Action execution handled by ActionExecutor
}
