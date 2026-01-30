package pl.nop.aiplayers.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import pl.nop.aiplayers.AIPlayersPlugin;
import pl.nop.aiplayers.economy.AIEconomyService;
import pl.nop.aiplayers.logging.AIPlayersFileLogger;
import pl.nop.aiplayers.model.AIBehaviorMode;
import pl.nop.aiplayers.model.AIControllerType;
import pl.nop.aiplayers.model.AIPlayerProfile;
import pl.nop.aiplayers.model.AIPlayerSession;
import pl.nop.aiplayers.npc.NPCHandle;
import pl.nop.aiplayers.npc.ProtocolLibNPCHandle;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AIPlayerManager {

    private final Plugin plugin;
    private final Map<String, AIPlayerProfile> profiles = new HashMap<>();
    private final Map<String, AIPlayerSession> sessions = new HashMap<>();
    private final AIEconomyService economyService;
    private final AIControllerType defaultControllerType;
    private final AIBehaviorMode defaultBehaviorMode;

    public AIPlayerManager(Plugin plugin, AIEconomyService economyService, AIControllerType defaultControllerType,
                           AIBehaviorMode defaultBehaviorMode) {
        this.plugin = plugin;
        this.economyService = economyService;
        this.defaultControllerType = defaultControllerType;
        this.defaultBehaviorMode = defaultBehaviorMode;
    }

    public synchronized AIPlayerProfile createProfile(String name, Location location, double roamRadius, String chatInstruction) {
        if (profiles.containsKey(name)) {
            return null;
        }
        UUID uuid = UUID.nameUUIDFromBytes(("AI-" + name).getBytes());
        AIPlayerProfile profile = new AIPlayerProfile(uuid, name, defaultControllerType, defaultBehaviorMode,
                location.clone(), location.clone(), roamRadius, chatInstruction);
        profiles.put(name, profile);
        economyService.createIfPossible(profile);
        plugin.getLogger().info("Created AI player profile for " + name);
        logToFile("Created AI player profile for " + name + " (uuid=" + uuid + ")");
        return profile;
    }

    public synchronized void addProfile(AIPlayerProfile profile) {
        if (profile == null || profiles.containsKey(profile.getName())) {
            return;
        }
        profiles.put(profile.getName(), profile);
        economyService.createIfPossible(profile);
        plugin.getLogger().info("Loaded AI player profile for " + profile.getName());
        logToFile("Loaded AI player profile for " + profile.getName() + " (uuid=" + profile.getUuid() + ")");
    }

    public synchronized int spawnStoredProfiles() {
        int spawned = 0;
        for (AIPlayerProfile profile : profiles.values()) {
            if (sessions.containsKey(profile.getName())) {
                continue;
            }
            Location spawnLocation = profile.getLastKnownLocation();
            if (spawnLocation == null) {
                spawnLocation = profile.getSpawnLocation();
            }
            if (spawnLocation == null || spawnLocation.getWorld() == null) {
                plugin.getLogger().warning("Skipping AI player " + profile.getName()
                        + " because no valid saved location was found.");
                continue;
            }
            if (profile.getSpawnLocation() == null) {
                profile.setSpawnLocation(spawnLocation.clone());
            }
            profile.setLastKnownLocation(spawnLocation.clone());
            NPCHandle npcHandle = new ProtocolLibNPCHandle(plugin, profile.getUuid(), profile.getName());
            npcHandle.spawn(spawnLocation);
            Inventory inventory = Bukkit.createInventory(null, 27, "AI " + profile.getName() + " Inventory");
            Inventory enderChest = Bukkit.createInventory(null, 27, "AI " + profile.getName() + " EnderChest");
            AIPlayerSession session = new AIPlayerSession(profile, npcHandle, inventory, enderChest);
            sessions.put(profile.getName(), session);
            spawned++;
            plugin.getLogger().info("Restored AI player " + profile.getName() + " at " + locationToString(spawnLocation));
            logToFile("Restored AI player " + profile.getName() + " at " + locationToString(spawnLocation));
        }
        return spawned;
    }

    public synchronized AIPlayerSession spawnAIPlayer(String name, Location spawnLocation, double roamRadius, String chatInstruction) {
        AIPlayerProfile profile = profiles.get(name);
        if (profile == null) {
            profile = createProfile(name, spawnLocation, roamRadius, chatInstruction);
            if (profile == null) {
                return null;
            }
        }
        if (sessions.containsKey(name)) {
            return sessions.get(name);
        }
        profile.setSpawnLocation(spawnLocation.clone());
        profile.setRoamRadius(roamRadius);
        profile.setChatInstruction(chatInstruction);
        profile.setLastKnownLocation(spawnLocation.clone());
        NPCHandle npcHandle = new ProtocolLibNPCHandle(plugin, profile.getUuid(), profile.getName());
        npcHandle.spawn(spawnLocation);
        Inventory inventory = Bukkit.createInventory(null, 27, "AI " + name + " Inventory");
        Inventory enderChest = Bukkit.createInventory(null, 27, "AI " + name + " EnderChest");
        AIPlayerSession session = new AIPlayerSession(profile, npcHandle, inventory, enderChest);
        sessions.put(name, session);
        plugin.getLogger().info("Spawned AI player " + name + " at " + locationToString(spawnLocation));
        logToFile("Spawned AI player " + name + " at " + locationToString(spawnLocation));
        return session;
    }

    public synchronized void despawnAIPlayer(String name) {
        AIPlayerSession session = sessions.remove(name);
        if (session != null) {
            session.getNpcHandle().despawn();
            plugin.getLogger().info("Despawned AI player " + name);
            logToFile("Despawned AI player " + name);
        }
    }

    public synchronized void removeAIPlayer(String name) {
        despawnAIPlayer(name);
        profiles.remove(name);
        logToFile("Removed AI player profile " + name);
    }

    public synchronized AIPlayerProfile getProfile(String name) {
        return profiles.get(name);
    }

    public synchronized AIPlayerSession getSession(String name) {
        return sessions.get(name);
    }

    public synchronized Collection<AIPlayerSession> getAllSessions() {
        return Collections.unmodifiableCollection(sessions.values());
    }

    public synchronized Collection<AIPlayerProfile> getAllProfiles() {
        return Collections.unmodifiableCollection(profiles.values());
    }

    public synchronized Optional<AIPlayerSession> getNearestSession(Location location, double radius) {
        return sessions.values().stream()
                .filter(session -> session.getNpcHandle().getLocation().getWorld().equals(location.getWorld()))
                .filter(session -> session.getNpcHandle().getLocation().distanceSquared(location) <= radius * radius)
                .findFirst();
    }

    public synchronized void despawnAll() {
        for (AIPlayerSession session : sessions.values()) {
            session.getNpcHandle().despawn();
        }
        sessions.clear();
    }

    private String locationToString(Location location) {
        return String.format("%s (%.1f, %.1f, %.1f)", location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }

    private void logToFile(String message) {
        AIPlayersFileLogger fileLogger = getFileLogger();
        if (fileLogger != null) {
            fileLogger.info(message);
        }
    }

    private AIPlayersFileLogger getFileLogger() {
        if (plugin instanceof AIPlayersPlugin) {
            return ((AIPlayersPlugin) plugin).getFileLogger();
        }
        return null;
    }
}
