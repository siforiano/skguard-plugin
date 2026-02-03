package com.skguard.modules.connection;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;

public class ConnectionManager implements SecurityModule, Listener {

    private final SKGuard plugin;
    private boolean enabled;

    private int throttleTime; // ms
    private int maxAccounts;

    // IP -> Timestamp of last connection attempt
    private final Map<InetAddress, Long> connectionAttempts;
    // IP -> Count of active connections (simplified for this context, ideally
    // tracked via persistent storage or online players)
    // For Phase 1, we will count players currently online from that IP.

    public ConnectionManager(SKGuard plugin) {
        this.plugin = plugin;
        this.connectionAttempts = new ConcurrentHashMap<>();
    }

    @Override
    public String getName() {
        return "ConnectionManager";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        reload();
    }

    @Override
    public void disable() {
        this.enabled = false;
        this.connectionAttempts.clear();
    }

    @Override
    public void reload() {
        this.throttleTime = plugin.getConfig().getInt("modules.ConnectionManager.connection-throttle", 2500);
        this.maxAccounts = plugin.getConfig().getInt("modules.ConnectionManager.max-accounts-per-ip", 3);
        this.connectionAttempts.clear();

        // Schedule periodic cleanup to prevent memory leaks
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            connectionAttempts.entrySet().removeIf(entry -> now - entry.getValue() > 300000); // 5 min
        }, 6000L, 6000L); // Every 5 minutes
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!enabled)
            return;

        InetAddress address = event.getAddress();
        String name = event.getName();

        // 1. Connection Throttling (Anti-Bot)
        long lastAttempt = connectionAttempts.getOrDefault(address, 0L);
        long now = System.currentTimeMillis();

        if (now - lastAttempt < throttleTime) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    ChatColor.RED + "You are connecting too fast! Please wait.");
            plugin.getLogManager().logWarn("IP " + address.getHostAddress() + " (" + name + ") throttled.");
            return;
        }
        connectionAttempts.put(address, now);

        // 2. Max Accounts Per IP
        long onlineCount = plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> {
                    java.net.InetSocketAddress addr = p.getAddress();
                    return addr != null && addr.getAddress() != null && addr.getAddress().equals(address);
                })
                .count();

        if (onlineCount >= maxAccounts) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    ChatColor.RED + "Too many accounts connected from your IP (" + maxAccounts + " max).");
            plugin.getLogManager().logWarn("IP " + address.getHostAddress() + " exceeded max accounts limit.");
            if (plugin.getDiscordWebhook() != null) {
                plugin.getDiscordWebhook().sendAlert("Connection Blocked",
                        "IP **" + address.getHostAddress() + "** blocked for exceeding max accounts.", 16711680);
            }
        }
    }
}

