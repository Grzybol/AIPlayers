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

    private final AIPlayerManager manager;
    public AIPlayersCommand(AIPlayerManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("aiplayers.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/aiplayers <add|remove|list|inspect>");
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
            sender.sendMessage(ChatColor.RED + "Usage: /aiplayers add <name>");
            return;
        }
        String name = args[1];
        Player player = (Player) sender;
        Location loc = player.getLocation();
        if (manager.getProfile(name) != null) {
            sender.sendMessage(ChatColor.RED + "AI player with that name already exists.");
            return;
        }
        manager.createProfile(name, loc);
        manager.spawnAIPlayer(name, loc);
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
}
