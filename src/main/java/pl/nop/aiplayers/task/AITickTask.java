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
import pl.nop.aiplayers.chat.engagement.ChatEngagementService;
import pl.nop.aiplayers.economy.AIEconomyService;
import pl.nop.aiplayers.manager.AIPlayerManager;
import pl.nop.aiplayers.model.AIPlayerSession;
import pl.nop.aiplayers.model.AIControllerType;
import pl.nop.aiplayers.npc.NPCHandle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class AITickTask extends BukkitRunnable {

    private final pl.nop.aiplayers.AIPlayersPlugin plugin;
    private final AIPlayerManager manager;
    private final AIControllerRegistry controllerRegistry;
    private final AIEconomyService economyService;
    private final AIChatService chatService;
    private final ActionExecutor actionExecutor;
    private final ChatEngagementService engagementService;
    private final Random random = new Random();
    private final double stepSize = 0.65;

    public AITickTask(pl.nop.aiplayers.AIPlayersPlugin plugin, AIPlayerManager manager, AIControllerRegistry controllerRegistry,
                      AIEconomyService economyService, AIChatService chatService, ActionExecutor actionExecutor,
                      ChatEngagementService engagementService) {
        this.plugin = plugin;
        this.manager = manager;
        this.controllerRegistry = controllerRegistry;
        this.economyService = economyService;
        this.chatService = chatService;
        this.actionExecutor = actionExecutor;
        this.engagementService = engagementService;
    }

    @Override
    public void run() {
        if (engagementService != null) {
            engagementService.tick(System.currentTimeMillis());
        }
        for (AIPlayerSession session : manager.getAllSessions()) {
            NPCHandle npc = session.getNpcHandle();
            if (npc.getLocation() == null) {
                continue;
            }
            lookAtNearestPlayer(session, npc.getLocation());
            actionExecutor.tick(session);
            Perception perception = buildPerception(session);
            AIController controller = controllerRegistry.getController(session.getProfile().getControllerType());
            CompletableFuture<Action> future = controller.decide(session, perception);
            future.thenAccept(action -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (action != null) {
                        actionExecutor.submit(session, action);
                    }
                    if (shouldApplyLocalMovement(session, action)) {
                        Action fallback = buildLocalMovementAction(session, npc.getLocation());
                        if (fallback != null) {
                            actionExecutor.submit(session, fallback);
                        }
                    }
                });
            });
        }
    }

    private void lookAtNearestPlayer(AIPlayerSession session, Location npcLocation) {
        if (npcLocation == null || npcLocation.getWorld() == null) {
            return;
        }
        Player nearest = null;
        double bestDistance = Double.MAX_VALUE;
        for (Player player : npcLocation.getWorld().getPlayers()) {
            double distance = player.getLocation().distanceSquared(npcLocation);
            if (distance < bestDistance) {
                bestDistance = distance;
                nearest = player;
            }
        }
        if (nearest != null) {
            Location target = nearest.getLocation().clone();
            target.setY(npcLocation.getY());
            session.getNpcHandle().lookAt(target);
        }
    }

    private boolean shouldApplyLocalMovement(AIPlayerSession session, Action action) {
        if (session.getProfile().getControllerType() == AIControllerType.REMOTE) {
            return true;
        }
        return action == null || action.getType() == pl.nop.aiplayers.ai.ActionType.IDLE;
    }

    private Action buildLocalMovementAction(AIPlayerSession session, Location current) {
        Location nextStep = nextWanderStep(session, current);
        if (nextStep != null) {
            return Action.moveTo(nextStep);
        }
        return Action.idle();
    }

    private Location nextWanderStep(AIPlayerSession session, Location current) {
        if (current == null) {
            return null;
        }
        Location spawn = session.getProfile().getSpawnLocation();
        if (spawn == null) {
            spawn = current.clone();
        }
        double radius = Math.max(session.getProfile().getRoamRadius(), 1.0);
        Location target = (Location) session.getRuntimeMemory().get("fallbackWanderTarget");
        if (target == null || target.getWorld() == null || target.distanceSquared(spawn) > radius * radius) {
            target = randomTarget(spawn, radius);
            session.getRuntimeMemory().put("fallbackWanderTarget", target);
        }
        double distance = target.distance(current);
        if (distance <= stepSize) {
            session.getRuntimeMemory().remove("fallbackWanderTarget");
            return target;
        }
        Location step = current.clone();
        double dx = target.getX() - current.getX();
        double dz = target.getZ() - current.getZ();
        double scale = stepSize / Math.sqrt(dx * dx + dz * dz);
        step.add(dx * scale, 0, dz * scale);
        step.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
        step.setPitch(0f);
        if (step.distanceSquared(spawn) > radius * radius) {
            session.getRuntimeMemory().remove("fallbackWanderTarget");
            return randomTarget(spawn, radius);
        }
        return step;
    }

    private Location randomTarget(Location center, double radius) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = Math.sqrt(random.nextDouble()) * radius;
        double x = center.getX() + Math.cos(angle) * distance;
        double z = center.getZ() + Math.sin(angle) * distance;
        return new Location(center.getWorld(), x, center.getY(), z);
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
