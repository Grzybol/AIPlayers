package pl.nop.aiplayers.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import pl.nop.aiplayers.model.AIPlayerProfile;

public class AIEconomyService {

    private final JavaPlugin plugin;
    private final boolean enabled;
    private Economy economy;

    public AIEconomyService(JavaPlugin plugin, boolean enabled) {
        this.plugin = plugin;
        this.enabled = enabled;
        setupEconomy();
    }

    private void setupEconomy() {
        if (!enabled) {
            plugin.getLogger().info("Economy integration disabled in config.");
            return;
        }
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault not found; economy features disabled.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("No economy provider found via Vault.");
            return;
        }
        economy = rsp.getProvider();
        plugin.getLogger().info("Hooked into economy provider: " + economy.getName());
    }

    public boolean isAvailable() {
        return economy != null && isEconomyProviderReady();
    }

    public void createIfPossible(AIPlayerProfile profile) {
        if (!isAvailable()) {
            return;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(profile.getUuid());
        if (!economy.hasAccount(offlinePlayer)) {
            economy.createPlayerAccount(offlinePlayer);
        }
    }

    public double getBalance(AIPlayerProfile profile) {
        if (!isAvailable()) {
            return 0;
        }
        return economy.getBalance(Bukkit.getOfflinePlayer(profile.getUuid()));
    }

    public void deposit(AIPlayerProfile profile, double amount) {
        if (isAvailable()) {
            economy.depositPlayer(Bukkit.getOfflinePlayer(profile.getUuid()), amount);
        }
    }

    public boolean withdraw(AIPlayerProfile profile, double amount) {
        if (!isAvailable()) {
            return false;
        }
        return economy.withdrawPlayer(Bukkit.getOfflinePlayer(profile.getUuid()), amount).transactionSuccess();
    }

    private boolean isEconomyProviderReady() {
        if (economy == null) {
            return false;
        }
        String economyName = economy.getName();
        if (economyName == null || !economyName.toLowerCase().contains("essentials")) {
            return true;
        }
        Plugin essentials = plugin.getServer().getPluginManager().getPlugin("Essentials");
        return essentials == null || essentials.isEnabled();
    }
}
