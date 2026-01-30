package pl.nop.aiplayers.ai;

import org.bukkit.Location;

import java.util.List;
import java.util.UUID;

public class Perception {
    private final String name;
    private final UUID uuid;
    private final String world;
    private final Location location;
    private final List<String> nearbyPlayers;
    private final List<String> nearbyAIPlayers;
    private final double balance;
    private final List<String> inventorySummary;
    private final List<String> chatHistory;
    private final long serverTimeTicks;

    public Perception(String name, UUID uuid, String world, Location location, List<String> nearbyPlayers, List<String> nearbyAIPlayers,
                      double balance, List<String> inventorySummary, List<String> chatHistory, long serverTimeTicks) {
        this.name = name;
        this.uuid = uuid;
        this.world = world;
        this.location = location;
        this.nearbyPlayers = nearbyPlayers;
        this.nearbyAIPlayers = nearbyAIPlayers;
        this.balance = balance;
        this.inventorySummary = inventorySummary;
        this.chatHistory = chatHistory;
        this.serverTimeTicks = serverTimeTicks;
    }

    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getWorld() {
        return world;
    }

    public Location getLocation() {
        return location;
    }

    public List<String> getNearbyPlayers() {
        return nearbyPlayers;
    }

    public List<String> getNearbyAIPlayers() {
        return nearbyAIPlayers;
    }

    public double getBalance() {
        return balance;
    }

    public List<String> getInventorySummary() {
        return inventorySummary;
    }

    public List<String> getChatHistory() {
        return chatHistory;
    }

    public long getServerTimeTicks() {
        return serverTimeTicks;
    }
}
