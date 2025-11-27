package pl.nop.aiplayers.model;

import org.bukkit.inventory.Inventory;
import pl.nop.aiplayers.npc.NPCHandle;

public class AIPlayerSession {

    private final AIPlayerProfile profile;
    private final NPCHandle npcHandle;
    private final Inventory inventory;
    private final Inventory enderChest;

    public AIPlayerSession(AIPlayerProfile profile, NPCHandle npcHandle, Inventory inventory, Inventory enderChest) {
        this.profile = profile;
        this.npcHandle = npcHandle;
        this.inventory = inventory;
        this.enderChest = enderChest;
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
}
