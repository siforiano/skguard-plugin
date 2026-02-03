package com.skguard.api;

import java.io.File;

import com.skguard.logger.LogManager;

/**
 * Unified interface for SKGuard across all platforms (Bukkit, Velocity).
 */
public interface SKGuardCore {
    
    org.slf4j.Logger getSlf4jLogger();
    
    java.util.logging.Logger getBukkitLogger();
    
    LogManager getLogManager();
    
    com.skguard.database.DatabaseManager getDatabaseManager();
    
    File getDataFolder();
    
    org.bukkit.configuration.file.FileConfiguration getConfig();
    
    void saveDefaultConfig();
    
    void saveResource(String resourcePath, boolean replace);
}

