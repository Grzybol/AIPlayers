package pl.nop.aiplayers.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
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
        return economy != null;
    }

    public void createIfPossible(AIPlayerProfile profile) {
        if (!isAvailable()) {
            return;
        }
        if (!economy.hasAccount(profile.getName())) {
            economy.createPlayerAccount(profile.getName());
        }
    }

    public double getBalance(AIPlayerProfile profile) {
        if (!isAvailable()) {
            return 0;
        }
        return economy.getBalance(profile.getName());
    }

    public void deposit(AIPlayerProfile profile, double amount) {
        if (isAvailable()) {
            economy.depositPlayer(profile.getName(), amount);
        }
    }

    public boolean withdraw(AIPlayerProfile profile, double amount) {
        if (!isAvailable()) {
            return false;
        }
        return economy.withdrawPlayer(profile.getName(), amount).transactionSuccess();
    }
}
