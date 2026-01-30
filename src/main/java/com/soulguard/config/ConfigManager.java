package com.soulguard.config;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;

import com.soulguard.SoulGuard;

public class ConfigManager {

    private final SoulGuard plugin;
    private FileConfiguration config;
    private File configFile;

    public ConfigManager(SoulGuard plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            plugin.getLogManager().logInfo("Default config.yml created.");
        }
        // Direct assignment from plugin's getConfig which is already updated by super.reloadConfig() or initially.
        this.config = plugin.getConfig();
        plugin.getLogManager().logInfo("Configuration loaded.");
    }

    public void reloadConfig() {
        // No call to plugin.reloadConfig() here to avoid loop
        this.config = plugin.getConfig();
        plugin.getLogManager().logInfo("Configuration reloaded.");
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
