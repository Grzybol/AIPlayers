package pl.nop.aiplayers.ai.controller;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import pl.nop.aiplayers.ai.Action;
import pl.nop.aiplayers.ai.Perception;
import pl.nop.aiplayers.model.AIPlayerSession;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class DummyAIController implements AIController {

    private final Random random = new Random();

    @Override
    public CompletableFuture<Action> decide(AIPlayerSession session, Perception perception) {
        return CompletableFuture.supplyAsync(() -> decideSync(session, perception));
    }

    private Action decideSync(AIPlayerSession session, Perception perception) {
        List<String> nearby = perception.getNearbyPlayers();
        if (!nearby.isEmpty() && random.nextDouble() < 0.3) {
            String targetName = nearby.get(0).split(":")[0];
            return Action.follow(targetName);
        }
        if (random.nextDouble() < 0.1) {
            return Action.say(randomChat());
        }
        Location base = perception.getLocation();
        World world = base.getWorld();
        double dx = (random.nextDouble() - 0.5) * 6;
        double dz = (random.nextDouble() - 0.5) * 6;
        Location target = new Location(world, base.getX() + dx, base.getY(), base.getZ() + dz);
        return Action.moveTo(target);
    }

    private String randomChat() {
        String[] msgs = new String[]{"hi", "o/", "what's up", "exploring...", "nice base!"};
        return msgs[random.nextInt(msgs.length)];
    }
}
