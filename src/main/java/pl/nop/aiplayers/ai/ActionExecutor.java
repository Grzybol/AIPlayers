package pl.nop.aiplayers.ai;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.nop.aiplayers.chat.AIChatService;
import pl.nop.aiplayers.model.AIPlayerSession;
import pl.nop.aiplayers.npc.NPCHandle;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ActionExecutor {

    private final AIChatService chatService;
    private final int maxQueueSize;
    private final long actionTimeoutMillis;
    private final long actionCooldownMillis;
    private final Map<UUID, Deque<QueuedAction>> queues = new HashMap<>();
    private final Map<UUID, Long> lastActionMillis = new HashMap<>();

    public ActionExecutor(AIChatService chatService, int maxQueueSize, long actionTimeoutMillis, long actionCooldownMillis) {
        this.chatService = chatService;
        this.maxQueueSize = maxQueueSize;
        this.actionTimeoutMillis = actionTimeoutMillis;
        this.actionCooldownMillis = actionCooldownMillis;
    }

    public void submit(AIPlayerSession session, Action action) {
        if (action == null || action.getType() == ActionType.IDLE) {
            return;
        }
        UUID uuid = session.getProfile().getUuid();
        long now = System.currentTimeMillis();
        Long last = lastActionMillis.get(uuid);
        if (last != null && now - last < actionCooldownMillis) {
            return;
        }
        Deque<QueuedAction> queue = queues.computeIfAbsent(uuid, key -> new ArrayDeque<>());
        if (!queue.isEmpty() && queue.peekLast().getAction().getType() == action.getType()) {
            queue.removeLast();
        }
        if (queue.size() >= maxQueueSize) {
            queue.removeFirst();
        }
        queue.addLast(new QueuedAction(action, now));
    }

    public void tick(AIPlayerSession session) {
        UUID uuid = session.getProfile().getUuid();
        Deque<QueuedAction> queue = queues.get(uuid);
        if (queue == null || queue.isEmpty()) {
            return;
        }
        QueuedAction queued = queue.peekFirst();
        long now = System.currentTimeMillis();
        if (now - queued.getCreatedAtMillis() > actionTimeoutMillis) {
            queue.removeFirst();
            return;
        }
        queue.removeFirst();
        executeAction(session, queued.getAction());
        lastActionMillis.put(uuid, now);
    }

    private void executeAction(AIPlayerSession session, Action action) {
        NPCHandle npc = session.getNpcHandle();
        switch (action.getType()) {
            case MOVE_TO:
                if (action.getTargetLocation() != null) {
                    npc.moveTo(action.getTargetLocation());
                }
                break;
            case FOLLOW_PLAYER:
                if (action.getTargetPlayer() != null && !action.getTargetPlayer().isEmpty()) {
                    Player target = Bukkit.getPlayerExact(action.getTargetPlayer());
                    if (target != null && target.getLocation().getWorld().equals(npc.getLocation().getWorld())) {
                        Location targetLoc = target.getLocation().clone();
                        targetLoc.setY(npc.getLocation().getY());
                        npc.moveTo(targetLoc);
                    }
                }
                break;
            case SAY:
                if (action.getMessage() != null) {
                    chatService.sendChatMessage(session, action.getMessage());
                }
                break;
            case BUY_ITEM:
            case SELL_ITEM:
            case CUSTOM:
            default:
                break;
        }
        session.getProfile().setLastKnownLocation(npc.getLocation());
    }

    private static class QueuedAction {
        private final Action action;
        private final long createdAtMillis;

        private QueuedAction(Action action, long createdAtMillis) {
            this.action = action;
            this.createdAtMillis = createdAtMillis;
        }

        public Action getAction() {
            return action;
        }

        public long getCreatedAtMillis() {
            return createdAtMillis;
        }
    }
}
