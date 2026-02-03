package com.skguard.logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;
import com.skguard.util.ColorUtil;

public class BypassTracker implements SecurityModule {

    private final SKGuard plugin;
    private boolean enabled;
    private File logFile;

    public BypassTracker(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "BypassTracker";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        this.logFile = new File(plugin.getDataFolder(), "logs/bypass_attempts.log");
        if (!logFile.getParentFile().exists()) {
            logFile.getParentFile().mkdirs();
        }
    }

    @Override
    public void disable() {
        this.enabled = false;
    }

    @Override
    public void reload() {
    }

    /**
     * Logs a bypass attempt and notifies staff.
     * 
     * @param player The player involved (can be null if it's a plugin-level bypass)
     * @param source The source of the bypass (e.g., "EventPriority-Bypass", "Reflection-Attempt")
     * @param detail Specific details about the attempt
     */
    public void logBypass(Player player, String source, String detail) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String playerName = (player != null) ? player.getName() : "SYSTEM/PLUGIN";
        String uuid = (player != null) ? player.getUniqueId().toString() : "N/A";

        String logEntry = String.format("[%s] [BYPASS-ATTEMPT] Player: %s (%s) | Source: %s | Detail: %s",
                timestamp, playerName, uuid, source, detail);

        // 1. Log to file
        try {
            Files.write(logFile.toPath(), (logEntry + "\n").getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            plugin.getLogManager().logError("Could not write to bypass log: " + e.getMessage());
        }

        // 2. Notify Console
        Bukkit.getConsoleSender().sendMessage(ColorUtil.translate("&c&l[SKGuard-SECURITY] &e" + logEntry));

        // 3. Notify Online Staff
        String alertMsg = ColorUtil.translate("&4&l[SKGuard-ALERT] &cIntento de Bypass detectado! &7Check console for details.");
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission("SKGuard.alerts")) {
                online.sendMessage(alertMsg);
            }
        }
    }
}

