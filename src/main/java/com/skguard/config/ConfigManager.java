package com.skguard.config;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.skguard.api.SKGuardCore;

public class ConfigManager {

    private final SKGuardCore plugin;
    private FileConfiguration config;

    public ConfigManager(SKGuardCore plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
        
        // Load Modular Files
        loadModularConfig("auth.yml");
        loadModularConfig("protection.yml");
        loadModularConfig("anticheat.yml");
        loadModularConfig("messages.yml");
        loadModularConfig("database.yml");
        loadModularConfig("advanced.yml");
        
        plugin.getLogManager().logInfo("Configuration loaded with modular support.");
    }

    private void loadModularConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        
        FileConfiguration moduleConfig = YamlConfiguration.loadConfiguration(file);
        
        // Merge into main config, allowing modular files to overwrite defaults
        for (String key : moduleConfig.getKeys(true)) {
            this.config.set(key, moduleConfig.get(key));
        }
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

