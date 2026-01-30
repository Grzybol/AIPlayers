package pl.nop.aiplayers.model;

import org.bukkit.inventory.Inventory;
import pl.nop.aiplayers.npc.NPCHandle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AIPlayerSession {

    private final AIPlayerProfile profile;
    private final NPCHandle npcHandle;
    private final Inventory inventory;
    private final Inventory enderChest;
    private final Map<String, Object> runtimeMemory;
    private final Map<String, Long> cooldowns;

    public AIPlayerSession(AIPlayerProfile profile, NPCHandle npcHandle, Inventory inventory, Inventory enderChest) {
        this.profile = profile;
        this.npcHandle = npcHandle;
        this.inventory = inventory;
        this.enderChest = enderChest;
        this.runtimeMemory = new ConcurrentHashMap<>();
        this.cooldowns = new ConcurrentHashMap<>();
    }

    public AIPlayerProfile getProfile() {
        return profile;
    }

    public NPCHandle getNpcHandle() {
        return npcHandle;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Inventory getEnderChest() {
        return enderChest;
    }

    public Map<String, Object> getRuntimeMemory() {
        return runtimeMemory;
    }

    public Map<String, Long> getCooldowns() {
        return cooldowns;
    }
}
