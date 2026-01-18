package pl.nop.aiplayers.storage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.nop.aiplayers.model.AIBehaviorMode;
import pl.nop.aiplayers.model.AIControllerType;
import pl.nop.aiplayers.model.AIPlayerProfile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AIPlayerStorage {

    private final File dataFolder;
    private final File profilesFile;

    public AIPlayerStorage(File dataFolder) {
        this.dataFolder = dataFolder;
        this.profilesFile = new File(dataFolder, "profiles.yml");
    }

    public void saveAll(Collection<AIPlayerProfile> profiles) {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            return;
        }
        YamlConfiguration config = new YamlConfiguration();
        for (AIPlayerProfile profile : profiles) {
            String basePath = "profiles." + profile.getName();
            config.set(basePath + ".uuid", profile.getUuid().toString());
            config.set(basePath + ".controller", profile.getControllerType().name());
            config.set(basePath + ".behavior", profile.getBehaviorMode().name());
            Location location = profile.getLastKnownLocation();
            if (location != null && location.getWorld() != null) {
                config.set(basePath + ".location.world", location.getWorld().getName());
                config.set(basePath + ".location.x", location.getX());
                config.set(basePath + ".location.y", location.getY());
                config.set(basePath + ".location.z", location.getZ());
                config.set(basePath + ".location.yaw", location.getYaw());
                config.set(basePath + ".location.pitch", location.getPitch());
            }
            if (!profile.getMetadata().isEmpty()) {
                config.createSection(basePath + ".metadata", profile.getMetadata());
            }
        }
        try {
            config.save(profilesFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public List<AIPlayerProfile> loadAll() {
        if (!profilesFile.exists()) {
            return Collections.emptyList();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(profilesFile);
        ConfigurationSection profilesSection = config.getConfigurationSection("profiles");
        if (profilesSection == null) {
            return Collections.emptyList();
        }
        List<AIPlayerProfile> profiles = new ArrayList<>();
        for (String name : profilesSection.getKeys(false)) {
            String basePath = "profiles." + name;
            String uuidString = config.getString(basePath + ".uuid");
            if (uuidString == null) {
                continue;
            }
            AIControllerType controllerType = parseControllerType(config.getString(basePath + ".controller"));
            AIBehaviorMode behaviorMode = parseBehaviorMode(config.getString(basePath + ".behavior"));
            Location location = loadLocation(config.getConfigurationSection(basePath + ".location"));
            AIPlayerProfile profile = new AIPlayerProfile(UUID.fromString(uuidString), name, controllerType, behaviorMode, location);
            ConfigurationSection metadataSection = config.getConfigurationSection(basePath + ".metadata");
            if (metadataSection != null) {
                for (Map.Entry<String, Object> entry : metadataSection.getValues(false).entrySet()) {
                    if (entry.getValue() != null) {
                        profile.getMetadata().put(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                }
            }
            profiles.add(profile);
        }
        return profiles;
    }

    private Location loadLocation(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String worldName = section.getString("world");
        if (worldName == null) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    private AIControllerType parseControllerType(String value) {
        if (value == null) {
            return AIControllerType.DUMMY;
        }
        try {
            return AIControllerType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return AIControllerType.DUMMY;
        }
    }

    private AIBehaviorMode parseBehaviorMode(String value) {
        if (value == null) {
            return AIBehaviorMode.WANDER;
        }
        try {
            return AIBehaviorMode.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return AIBehaviorMode.WANDER;
        }
    }
}
