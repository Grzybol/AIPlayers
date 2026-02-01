package pl.nop.aiplayers.command;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.nop.aiplayers.manager.AIPlayerManager;
import pl.nop.aiplayers.model.AIPlayerSession;

public class AIPlayersCommand implements CommandExecutor {

    private final pl.nop.aiplayers.AIPlayersPlugin plugin;
    private final AIPlayerManager manager;
    public AIPlayersCommand(pl.nop.aiplayers.AIPlayersPlugin plugin, AIPlayerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aiplayers.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/aiplayers <add|remove|list|inspect|reload>");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "add":
                handleAdd(sender, args);
                break;
            case "remove":
                handleRemove(sender, args);
                break;
            case "list":
                handleList(sender);
                break;
            case "inspect":
                handleInspect(sender, args);
                break;
            case "reload":
                handleReload(sender);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand");
        }
        return true;
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can add AI players.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /aiplayers add <name> <radius> '<chat instruction>'");
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /aiplayers add <name> <radius> '<chat instruction>'");
            return;
        }
        String name = args[1];
        if (name.length() > AIPlayerManager.MAX_NAME_LENGTH) {
            sender.sendMessage(ChatColor.RED + "AI player name cannot be longer than "
                    + AIPlayerManager.MAX_NAME_LENGTH + " characters.");
            return;
        }
        double radius;
        try {
            radius = Double.parseDouble(args[2]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Radius must be a number.");
            return;
        }
        String instruction = parseInstruction(args, 3);
        Player player = (Player) sender;
        Location loc = player.getLocation();
        if (manager.getProfile(name) != null) {
            sender.sendMessage(ChatColor.RED + "AI player with that name already exists.");
            return;
        }
        manager.createProfile(name, loc, radius, instruction);
        manager.spawnAIPlayer(name, loc, radius, instruction);
        sender.sendMessage(ChatColor.GREEN + "Spawned AI player " + name + ".");
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /aiplayers remove <name>");
            return;
        }
        String name = args[1];
        manager.removeAIPlayer(name);
        sender.sendMessage(ChatColor.YELLOW + "Removed AI player " + name + ".");
    }

    private void handleList(CommandSender sender) {
        if (manager.getAllSessions().isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No AI players active.");
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "AI Players:");
        for (AIPlayerSession session : manager.getAllSessions()) {
            String msg = String.format("- %s at %s (controller: %s, behavior: %s)",
                    session.getProfile().getName(),
                    session.getNpcHandle().getLocation().getWorld().getName() + " " + session.getNpcHandle().getLocation().getBlockX() + "," + session.getNpcHandle().getLocation().getBlockY() + "," + session.getNpcHandle().getLocation().getBlockZ(),
                    session.getProfile().getControllerType().name(),
                    session.getProfile().getBehaviorMode().name());
            sender.sendMessage(ChatColor.YELLOW + msg);
        }
    }

    private void handleInspect(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can inspect inventories.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /aiplayers inspect <name>");
            return;
        }
        String name = args[1];
        AIPlayerSession session = manager.getSession(name);
        if (session == null) {
            sender.sendMessage(ChatColor.RED + "AI player not found.");
            return;
        }
        ((Player) sender).openInventory(session.getInventory());
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadPluginConfig();
        sender.sendMessage(ChatColor.GREEN + "AIPlayers configuration reloaded.");
    }

    private String parseInstruction(String[] args, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        String raw = builder.toString().trim();
        if (raw.startsWith("'") && raw.endsWith("'") && raw.length() > 1) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }
}
