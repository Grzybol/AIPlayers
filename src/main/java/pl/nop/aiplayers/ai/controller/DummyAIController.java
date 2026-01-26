package pl.nop.aiplayers.ai.controller;

import org.bukkit.Location;
import pl.nop.aiplayers.ai.Action;
import pl.nop.aiplayers.ai.Perception;
import pl.nop.aiplayers.model.AIPlayerSession;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class DummyAIController implements AIController {

    private final Random random = new Random();
    private final int chatMemorySize;
    private final double stepSize;
    private final double lookChance;
    private final double chatReplyChance;

    public DummyAIController(int chatMemorySize) {
        this.chatMemorySize = Math.max(chatMemorySize, 1);
        this.stepSize = 0.65;
        this.lookChance = 0.25;
        this.chatReplyChance = 0.35;
    }

    @Override
    public CompletableFuture<Action> decide(AIPlayerSession session, Perception perception) {
        return CompletableFuture.supplyAsync(() -> decideSync(session, perception));
    }

    private Action decideSync(AIPlayerSession session, Perception perception) {
        ChatLine newestIncoming = updateChatMemory(session, perception.getChatHistory(), perception.getName());
        if (newestIncoming != null && shouldReply(session, newestIncoming)) {
            String reply = buildReply(session, newestIncoming);
            if (reply != null && !reply.isBlank()) {
                session.getRuntimeMemory().put("lastRespondedLine", newestIncoming.rawLine);
                return Action.say(reply);
            }
        }
        if (random.nextDouble() < lookChance) {
            Location lookTarget = randomLookTarget(session, perception.getLocation());
            if (lookTarget != null) {
                return Action.lookAt(lookTarget);
            }
        }
        Location nextStep = nextWanderStep(session, perception.getLocation());
        if (nextStep != null) {
            return Action.moveTo(nextStep);
        }
        return Action.idle();
    }

    private ChatLine updateChatMemory(AIPlayerSession session, List<String> history, String selfName) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        Object lastSeenObj = session.getRuntimeMemory().get("lastChatLine");
        String lastSeen = lastSeenObj instanceof String ? (String) lastSeenObj : null;
        int startIndex = 0;
        if (lastSeen != null) {
            int idx = history.lastIndexOf(lastSeen);
            if (idx >= 0) {
                startIndex = idx + 1;
            } else {
                startIndex = Math.max(0, history.size() - chatMemorySize);
            }
        }
        @SuppressWarnings("unchecked")
        Deque<ChatLine> memory = (Deque<ChatLine>) session.getRuntimeMemory()
                .computeIfAbsent("chatMemory", key -> new ArrayDeque<>());
        @SuppressWarnings("unchecked")
        Map<String, String> lastBySpeaker = (Map<String, String>) session.getRuntimeMemory()
                .computeIfAbsent("lastMessageBySpeaker", key -> new LinkedHashMap<>());
        ChatLine newest = null;
        for (int i = startIndex; i < history.size(); i++) {
            String line = history.get(i);
            ChatLine parsed = parseLine(line);
            if (parsed == null) {
                continue;
            }
            if (parsed.speaker.equalsIgnoreCase(selfName)) {
                continue;
            }
            memory.addLast(parsed);
            if (memory.size() > chatMemorySize) {
                memory.removeFirst();
            }
            lastBySpeaker.put(parsed.speaker, parsed.message);
            if (lastBySpeaker.size() > chatMemorySize) {
                String oldest = lastBySpeaker.keySet().iterator().next();
                lastBySpeaker.remove(oldest);
            }
            newest = parsed;
        }
        session.getRuntimeMemory().put("lastChatLine", history.get(history.size() - 1));
        return newest;
    }

    private boolean shouldReply(AIPlayerSession session, ChatLine line) {
        String lastResponded = (String) session.getRuntimeMemory().get("lastRespondedLine");
        if (line.rawLine.equals(lastResponded)) {
            return false;
        }
        String name = session.getProfile().getName();
        String lower = line.message.toLowerCase();
        if (lower.contains(name.toLowerCase())) {
            return true;
        }
        if (lower.contains("?") || lower.contains("help") || lower.contains("pomoc")) {
            return random.nextDouble() < 0.8;
        }
        return random.nextDouble() < chatReplyChance;
    }

    private String buildReply(AIPlayerSession session, ChatLine line) {
        String instruction = session.getProfile().getChatInstruction();
        String base = buildBaseReply(session, line);
        return applyInstructionStyle(base, instruction, session);
    }

    private String buildBaseReply(AIPlayerSession session, ChatLine line) {
        String speaker = line.speaker;
        String message = line.message;
        String lower = message.toLowerCase();
        String prefix = "@" + speaker + " ";
        String response;
        if (lower.contains("hej") || lower.contains("hi") || lower.contains("hello") || lower.contains("cześć")) {
            response = prefix + "hej! co tam u ciebie?";
        } else if (lower.contains("?")) {
            response = prefix + "dobre pytanie. co dokładnie masz na myśli?";
        } else if (lower.contains(session.getProfile().getName().toLowerCase())) {
            response = prefix + "słyszę, że mnie wołasz. w czym pomóc?";
        } else {
            response = prefix + "rozumiem. brzmi ciekawie.";
        }
        String contextual = appendContextHint(session, speaker);
        if (!contextual.isEmpty()) {
            response += " " + contextual;
        }
        return response;
    }

    private String appendContextHint(AIPlayerSession session, String speaker) {
        @SuppressWarnings("unchecked")
        Map<String, String> lastBySpeaker = (Map<String, String>) session.getRuntimeMemory().get("lastMessageBySpeaker");
        if (lastBySpeaker == null || lastBySpeaker.isEmpty()) {
            return "";
        }
        String lastMessage = lastBySpeaker.get(speaker);
        if (lastMessage == null || lastMessage.isBlank()) {
            return "";
        }
        return "Pamiętam, że pisałeś: \"" + shorten(lastMessage, 40) + "\".";
    }

    private String applyInstructionStyle(String response, String instruction, AIPlayerSession session) {
        if (instruction == null || instruction.isBlank()) {
            return response;
        }
        String styled = response;
        String lower = instruction.toLowerCase();
        if (lower.contains("formal")) {
            styled = styled.replace("hej", "dzień dobry").replace("co tam", "jak się Pan/Pani ma");
        }
        if (lower.contains("luź") || lower.contains("casual") || lower.contains("slang")) {
            styled = styled.replace("rozumiem", "spoko").replace("brzmi ciekawie", "brzmi spoko");
        }
        if (lower.contains("krót") || lower.contains("zwięź")) {
            styled = shorten(styled, 70);
        } else if (lower.contains("szczeg") || lower.contains("dłu")) {
            styled += " Jeśli chcesz, mogę doprecyzować albo dopytać resztę.";
        }
        if (lower.contains("emoji") || lower.contains("emot")) {
            styled += " 🙂";
        }
        if (lower.contains("pytaj")) {
            styled += " Masz jeszcze jakieś pytanie?";
        }
        return styled;
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
        Location target = (Location) session.getRuntimeMemory().get("wanderTarget");
        if (target == null || target.getWorld() == null || target.distanceSquared(spawn) > radius * radius) {
            target = randomTarget(spawn, radius);
            session.getRuntimeMemory().put("wanderTarget", target);
        }
        double distance = target.distance(current);
        if (distance <= stepSize) {
            session.getRuntimeMemory().remove("wanderTarget");
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
            session.getRuntimeMemory().remove("wanderTarget");
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

    private Location randomLookTarget(AIPlayerSession session, Location current) {
        Location spawn = session.getProfile().getSpawnLocation();
        if (spawn == null) {
            spawn = current;
        }
        if (spawn == null) {
            return null;
        }
        double radius = Math.max(session.getProfile().getRoamRadius(), 1.0);
        Location target = randomTarget(spawn, radius);
        target.setY(current != null ? current.getY() : spawn.getY());
        return target;
    }

    private ChatLine parseLine(String line) {
        if (line == null) {
            return null;
        }
        int idx = line.indexOf(":");
        if (idx <= 0 || idx >= line.length() - 1) {
            return null;
        }
        String speaker = line.substring(0, idx).trim();
        String message = line.substring(idx + 1).trim();
        if (speaker.isEmpty() || message.isEmpty()) {
            return null;
        }
        return new ChatLine(line, speaker, message);
    }

    private String shorten(String message, int limit) {
        if (message == null) {
            return "";
        }
        if (message.length() <= limit) {
            return message;
        }
        return message.substring(0, Math.max(0, limit - 3)) + "...";
    }

    private static class ChatLine {
        private final String rawLine;
        private final String speaker;
        private final String message;

        private ChatLine(String rawLine, String speaker, String message) {
            this.rawLine = rawLine;
            this.speaker = speaker;
            this.message = message;
        }
    }
}
