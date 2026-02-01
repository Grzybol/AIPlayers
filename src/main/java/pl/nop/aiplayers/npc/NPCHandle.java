package pl.nop.aiplayers.npc;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface NPCHandle {
    void spawn(Location location);
    void despawn();
    void moveTo(Location target);
    void lookAt(Location target);
    Location getLocation();
    void teleport(Location location);
    void showTo(Player player);
}
