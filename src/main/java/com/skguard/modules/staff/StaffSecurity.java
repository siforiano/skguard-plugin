package com.skguard.modules.staff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;

public class StaffSecurity implements SecurityModule, Listener {

    private final SKGuard plugin;
    private boolean enabled;
    private final Map<String, String> staffIps; // Username -> IP
    private final Map<UUID, String> activeSessions; // UUID -> Session IP
    private boolean enableIpWhitelist;
    private boolean enableDriftLock;

    public StaffSecurity(SKGuard plugin) {
        this.plugin = plugin;
        this.staffIps = new HashMap<>();
        this.activeSessions = new HashMap<>();
        this.enableIpWhitelist = true;
        this.enableDriftLock = true;
    }

    @Override
    public String getName() {
        return "StaffSecurity";
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
        this.staffIps.clear();
    }

    @Override
    public void reload() {
        this.staffIps.clear();
        this.activeSessions.clear();
        this.enableIpWhitelist = plugin.getConfig().getBoolean("modules.StaffSecurity.ip-whitelist.enabled", true);
        this.enableDriftLock = plugin.getConfig().getBoolean("modules.StaffSecurity.ip-drift-lock", true);

        List<String> staffList = plugin.getConfig().getStringList("modules.StaffSecurity.ip-whitelist.accounts");
        for (String entry : staffList) {
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                this.staffIps.put(parts[0].toLowerCase(), parts[1]);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        Player player = event.getPlayer();
        if ((player.hasPermission("SKGuard.staff") || staffIps.containsKey(player.getName().toLowerCase())) && player.getAddress() != null && player.getAddress().getAddress() != null) {
            activeSessions.put(player.getUniqueId(), player.getAddress().getAddress().getHostAddress());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        activeSessions.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(org.bukkit.event.player.PlayerMoveEvent event) {
        if (!enabled || !enableDriftLock) return;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        if (activeSessions.containsKey(uuid) && player.getAddress() != null && player.getAddress().getAddress() != null) {
            String initialIp = activeSessions.get(uuid);
            String currentIp = player.getAddress().getAddress().getHostAddress();
            
            if (!initialIp.equals(currentIp)) {
                event.setCancelled(true);
                player.kickPlayer(ChatColor.RED + "[SKGuard] IP Drift Detected. Session terminated for safety.");
                plugin.getLogManager().logWarn("IP DRIFT BLOCKED: Staff " + player.getName() + " changed IP mid-session! (" + initialIp + " -> " + currentIp + ")");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAsyncLogin(AsyncPlayerPreLoginEvent event) {
        if (!enabled || !enableIpWhitelist)
            return;

        String name = event.getName().toLowerCase();
        String ip = event.getAddress().getHostAddress();

        // If player is in the staff configuration
        if (staffIps.containsKey(name)) {
            String allowedIp = staffIps.get(name);
            if (!ip.equals(allowedIp)) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                        ChatColor.RED + "Security Alert: You are joining from an unauthorized IP.");

                plugin.getLogManager().logError("Staff " + name + " tried to join from unauthorized IP: " + ip);
                if (plugin.getDiscordWebhook() != null) {
                    plugin.getDiscordWebhook().sendAlert("Staff Security",
                            "**SECURITY ALERT**: Staff **" + name + "** tried to join from unauthorized IP `" + ip
                                    + "` (Expected: `" + allowedIp + "`)",
                            16711680);
                }
            }
        }
    }
}

