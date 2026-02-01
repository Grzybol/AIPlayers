package pl.nop.aiplayers.npc;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;

public class ProtocolLibNPCHandle implements NPCHandle {

    private final Plugin plugin;
    private final UUID uuid;
    private final String name;
    private final ProtocolManager manager;
    private final int entityId;
    private Location location;
    private boolean spawned = false;

    public ProtocolLibNPCHandle(Plugin plugin, UUID uuid, String name) {
        this.plugin = plugin;
        this.uuid = uuid;
        this.name = name;
        this.manager = ProtocolLibrary.getProtocolManager();
        this.entityId = new Random().nextInt(Integer.MAX_VALUE / 2);
    }

    @Override
    public void spawn(Location location) {
        this.location = location.clone();
        if (manager == null) {
            plugin.getLogger().warning("ProtocolLib not found - cannot spawn NPC " + name);
            return;
        }
        sendPlayerInfo(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        sendNamedEntitySpawn();
        spawned = true;
    }

    @Override
    public void despawn() {
        if (!spawned || manager == null) {
            return;
        }
        PacketContainer destroy = manager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
        destroy.getIntLists().write(0, Collections.singletonList(entityId));
        broadcastPacket(destroy);
        sendPlayerInfo(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
        spawned = false;
    }

    @Override
    public void moveTo(Location target) {
        if (!spawned || manager == null) {
            return;
        }
        this.location = target.clone();
        PacketContainer teleport = manager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
        teleport.getIntegers().write(0, entityId);
        teleport.getDoubles().write(0, target.getX());
        teleport.getDoubles().write(1, target.getY());
        teleport.getDoubles().write(2, target.getZ());
        teleport.getBytes().write(0, (byte) ((target.getYaw() % 360) * 256 / 360));
        teleport.getBytes().write(1, (byte) ((target.getPitch() % 360) * 256 / 360));
        broadcastPacket(teleport);
        sendHeadRotation(target);
    }

    @Override
    public void lookAt(Location target) {
        if (!spawned || manager == null || location == null) {
            return;
        }
        Location clone = location.clone();
        clone.setDirection(target.toVector().subtract(clone.toVector()));
        sendHeadRotation(clone);
    }

    @Override
    public Location getLocation() {
        return location == null ? null : location.clone();
    }

    @Override
    public void teleport(Location location) {
        moveTo(location);
    }

    @Override
    public void showTo(Player player) {
        if (player == null || manager == null || location == null) {
            return;
        }
        sendPlayerInfo(EnumWrappers.PlayerInfoAction.ADD_PLAYER, player);
        sendNamedEntitySpawn(player);
        sendHeadRotation(location, player);
    }

    private void sendPlayerInfo(EnumWrappers.PlayerInfoAction action) {
        PacketContainer packet = manager.createPacket(PacketType.Play.Server.PLAYER_INFO);
        WrappedGameProfile profile = new WrappedGameProfile(uuid, name);
        PlayerInfoData data = new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(name));
        packet.getPlayerInfoAction().write(0, action);
        packet.getPlayerInfoDataLists().write(0, Collections.singletonList(data));
        broadcastPacket(packet);
    }

    private void sendPlayerInfo(EnumWrappers.PlayerInfoAction action, Player player) {
        PacketContainer packet = manager.createPacket(PacketType.Play.Server.PLAYER_INFO);
        WrappedGameProfile profile = new WrappedGameProfile(uuid, name);
        PlayerInfoData data = new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL, WrappedChatComponent.fromText(name));
        packet.getPlayerInfoAction().write(0, action);
        packet.getPlayerInfoDataLists().write(0, Collections.singletonList(data));
        sendPacket(player, packet);
    }

    private void sendNamedEntitySpawn() {
        PacketContainer packet = manager.createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
        packet.getIntegers().write(0, entityId);
        packet.getUUIDs().write(0, uuid);
        packet.getDoubles().write(0, location.getX());
        packet.getDoubles().write(1, location.getY());
        packet.getDoubles().write(2, location.getZ());
        packet.getBytes().write(0, (byte) ((location.getYaw() % 360) * 256 / 360));
        packet.getBytes().write(1, (byte) ((location.getPitch() % 360) * 256 / 360));
        broadcastPacket(packet);
    }

    private void sendNamedEntitySpawn(Player player) {
        PacketContainer packet = manager.createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
        packet.getIntegers().write(0, entityId);
        packet.getUUIDs().write(0, uuid);
        packet.getDoubles().write(0, location.getX());
        packet.getDoubles().write(1, location.getY());
        packet.getDoubles().write(2, location.getZ());
        packet.getBytes().write(0, (byte) ((location.getYaw() % 360) * 256 / 360));
        packet.getBytes().write(1, (byte) ((location.getPitch() % 360) * 256 / 360));
        sendPacket(player, packet);
    }

    private void sendHeadRotation(Location target) {
        PacketContainer head = manager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        head.getIntegers().write(0, entityId);
        head.getBytes().write(0, (byte) ((target.getYaw() % 360) * 256 / 360));
        broadcastPacket(head);
    }

    private void sendHeadRotation(Location target, Player player) {
        PacketContainer head = manager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        head.getIntegers().write(0, entityId);
        head.getBytes().write(0, (byte) ((target.getYaw() % 360) * 256 / 360));
        sendPacket(player, head);
    }

    private void broadcastPacket(PacketContainer packet) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            try {
                manager.sendServerPacket(online, packet);
            } catch (InvocationTargetException e) {
                plugin.getLogger().warning("Failed to send NPC packet: " + e.getMessage());
            }
        }
    }

    private void sendPacket(Player player, PacketContainer packet) {
        try {
            manager.sendServerPacket(player, packet);
        } catch (InvocationTargetException e) {
            plugin.getLogger().warning("Failed to send NPC packet: " + e.getMessage());
        }
    }
}
