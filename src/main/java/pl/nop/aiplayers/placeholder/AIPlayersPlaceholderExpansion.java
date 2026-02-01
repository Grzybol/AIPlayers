package pl.nop.aiplayers.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import pl.nop.aiplayers.manager.AIPlayerManager;

public class AIPlayersPlaceholderExpansion extends PlaceholderExpansion {

    private final AIPlayerManager aiPlayerManager;
    private final String version;

    public AIPlayersPlaceholderExpansion(AIPlayerManager aiPlayerManager, String version) {
        this.aiPlayerManager = aiPlayerManager;
        this.version = version;
    }

    @Override
    public String getIdentifier() {
        return "aiplayers";
    }

    @Override
    public String getAuthor() {
        return "nop";
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (aiPlayerManager == null) {
            return "0";
        }
        if ("bots".equalsIgnoreCase(params) || "bot".equalsIgnoreCase(params) || "online_bots".equalsIgnoreCase(params)) {
            return String.valueOf(aiPlayerManager.getOnlineSessionCount());
        }
        if ("online".equalsIgnoreCase(params)
                || "online_total".equalsIgnoreCase(params)
                || "total_online".equalsIgnoreCase(params)
                || "total".equalsIgnoreCase(params)) {
            return String.valueOf(aiPlayerManager.getTotalOnlineCount());
        }
        if ("humans".equalsIgnoreCase(params) || "players".equalsIgnoreCase(params)) {
            return String.valueOf(Bukkit.getOnlinePlayers().size());
        }
        return null;
    }
}
