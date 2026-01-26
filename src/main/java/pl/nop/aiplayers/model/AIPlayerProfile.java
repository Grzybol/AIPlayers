package pl.nop.aiplayers.model;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AIPlayerProfile {

    private final UUID uuid;
    private final String name;
    private AIControllerType controllerType;
    private AIBehaviorMode behaviorMode;
    private Location lastKnownLocation;
    private Location spawnLocation;
    private double roamRadius;
    private String chatInstruction;
    private final Map<String, String> metadata;

    public AIPlayerProfile(UUID uuid, String name, AIControllerType controllerType, AIBehaviorMode behaviorMode,
                           Location lastKnownLocation, Location spawnLocation, double roamRadius, String chatInstruction) {
        this.uuid = uuid;
        this.name = name;
        this.controllerType = controllerType;
        this.behaviorMode = behaviorMode;
        this.lastKnownLocation = lastKnownLocation;
        this.spawnLocation = spawnLocation;
        this.roamRadius = roamRadius;
        this.chatInstruction = chatInstruction;
        this.metadata = new HashMap<>();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public AIControllerType getControllerType() {
        return controllerType;
    }

    public void setControllerType(AIControllerType controllerType) {
        this.controllerType = controllerType;
    }

    public AIBehaviorMode getBehaviorMode() {
        return behaviorMode;
    }

    public void setBehaviorMode(AIBehaviorMode behaviorMode) {
        this.behaviorMode = behaviorMode;
    }

    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    public void setLastKnownLocation(Location lastKnownLocation) {
        this.lastKnownLocation = lastKnownLocation;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }

    public double getRoamRadius() {
        return roamRadius;
    }

    public void setRoamRadius(double roamRadius) {
        this.roamRadius = roamRadius;
    }

    public String getChatInstruction() {
        return chatInstruction;
    }

    public void setChatInstruction(String chatInstruction) {
        this.chatInstruction = chatInstruction;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
