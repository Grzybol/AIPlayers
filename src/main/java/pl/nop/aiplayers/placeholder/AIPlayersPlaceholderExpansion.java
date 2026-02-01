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
        if ("bots".equalsIgnoreCase(params) || "online".equalsIgnoreCase(params)) {
            return String.valueOf(aiPlayerManager.getOnlineSessionCount());
        }
        if ("online_total".equalsIgnoreCase(params) || "total_online".equalsIgnoreCase(params)) {
            int bots = aiPlayerManager.getOnlineSessionCount();
            int humans = Bukkit.getOnlinePlayers().size();
            return String.valueOf(humans + bots);
        }
        return null;
    }
}
