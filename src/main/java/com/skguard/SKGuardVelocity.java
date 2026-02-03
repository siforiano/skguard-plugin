package com.skguard;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

/**
 * Entry point for SKGuard on Velocity.
 */
import com.skguard.api.SKGuardCore;
import com.skguard.logger.LogManager;
import com.skguard.database.DatabaseManager;
import com.skguard.config.ConfigManager;
import com.skguard.api.platform.SKGuardPlatform;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;

@Plugin(
        id = "skguard",
        name = "SKGuard",
        version = "1.0-SNAPSHOT",
        description = "Advanced Security for Minecraft Networks",
        authors = {"SKGuardTeam"}
)
public class SKGuardVelocity implements SKGuardCore {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private LogManager logManager;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private SKGuardPlatform platform;

    @Inject
    public SKGuardVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // 1. Initialize Aesthetics
        com.skguard.util.ConsoleUtil.initialize(logger);
        com.skguard.util.ConsoleUtil.sendBanner();
        com.skguard.util.ConsoleUtil.phase("Velocity Network Initialization");

        // 2. Initialize Platform
        this.platform = new VelocityPlatform(this);
        this.logManager = new LogManager(this);

        // 3. Prepare Data Directory
        saveDefaultConfig();

        // 4. Initialize Core
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();

        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();

        com.skguard.util.ConsoleUtil.info("Velocity Network Bridge fully operational.");
        com.skguard.util.ConsoleUtil.phase("Network Protection Active");
    }

    @Override
    public org.slf4j.Logger getSlf4jLogger() {
        return logger;
    }

    @Override
    public java.util.logging.Logger getBukkitLogger() {
        return null;
    }

    @Override
    public LogManager getLogManager() {
        return logManager;
    }

    @Override
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    @Override
    public File getDataFolder() {
        return dataDirectory.toFile();
    }

    @Override
    public FileConfiguration getConfig() {
        return configManager != null ? configManager.getConfig() : null;
    }

    @Override
    public void saveDefaultConfig() {
        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (java.io.IOException e) {
                logger.error("Could not create data directory: " + e.getMessage());
            }
        }

        Path configFile = dataDirectory.resolve("config.yml");
        if (!Files.exists(configFile)) {
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile);
                    logger.info("Default config.yml created at " + configFile.toString());
                }
            } catch (java.io.IOException e) {
                logger.error("Could not save default config.yml: " + e.getMessage());
            }
        }
    }

    @Override
    public void saveResource(String resourcePath, boolean replace) {
        Path target = dataDirectory.resolve(resourcePath);
        if (Files.exists(target) && !replace) return;
        
        try (InputStream in = getClass().getResourceAsStream("/" + resourcePath)) {
            if (in != null) {
                Files.createDirectories(target.getParent());
                Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (java.io.IOException e) {
            logger.error("Could not save resource " + resourcePath + ": " + e.getMessage());
        }
    }

    public ProxyServer getServer() {
        return server;
    }
}

