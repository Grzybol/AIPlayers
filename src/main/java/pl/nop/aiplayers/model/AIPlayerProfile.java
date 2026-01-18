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
    private final Map<String, String> metadata;

    public AIPlayerProfile(UUID uuid, String name, AIControllerType controllerType, AIBehaviorMode behaviorMode, Location lastKnownLocation) {
        this.uuid = uuid;
        this.name = name;
        this.controllerType = controllerType;
        this.behaviorMode = behaviorMode;
        this.lastKnownLocation = lastKnownLocation;
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

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
