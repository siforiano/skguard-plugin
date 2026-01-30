package com.soulguard;

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
 * Entry point for SoulGuard on Velocity.
 */
@Plugin(
        id = "soulguard",
        name = "SoulGuard",
        version = "1.0-SNAPSHOT",
        description = "Advanced Security for Minecraft Networks",
        authors = {"SoulGuardTeam"}
)
public class SoulGuardVelocity {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public SoulGuardVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        saveDefaultConfig();
        logger.info("SoulGuard Velocity Bridge initialized!");
    }

    private void saveDefaultConfig() {
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
                } else {
                    logger.warn("Could not find config.yml in JAR resources!");
                }
            } catch (java.io.IOException e) {
                logger.error("Could not save default config.yml: " + e.getMessage());
            }
        }
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }
}
