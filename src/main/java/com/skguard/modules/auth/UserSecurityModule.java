package com.skguard.modules.auth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;
import com.skguard.util.ColorUtil;

public class UserSecurityModule implements SecurityModule, Listener {

    private boolean enabled;
    private final Set<UUID> lockedAccounts = new HashSet<>();
    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastKnownIp = new HashMap<>();

    private final SKGuard plugin;

    public UserSecurityModule(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "UserSecurity";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
    }

    @Override
    public void disable() {
        this.enabled = false;
        lockedAccounts.clear();
        failedAttempts.clear();
        lastKnownIp.clear();
    }

    @Override
    public void reload() {
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        // We keep lastKnownIp for session consistency during the session, 
        // but it could be cleared if not needed for offline checks.
        // For now, let's keep it but ensure failedAttempts doesn't grow indefinitely
        if (failedAttempts.size() > 1000) failedAttempts.clear(); 
    }

    public void setLocked(UUID uuid, boolean locked) {
        if (locked)
            lockedAccounts.add(uuid);
        else
            lockedAccounts.remove(uuid);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        if (!enabled)
            return;

        String ip = event.getAddress().getHostAddress();

        // Brute Force Protection
        if (failedAttempts.getOrDefault(ip, 0) >= 5) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    ColorUtil.translate("&c[SKGuard] Your connection is temporarily suspended. Try again later."));
            return;
        }

        // Session Lock
        // In a real scenario, we check UUID persistence (MySQL/Config)
        if (lockedAccounts.contains(event.getUniqueId())) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    ColorUtil.translate(
                            "&c[SKGuard] Authentication error. Please contact a server administrator."));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled)
            return;

        Player player = event.getPlayer();
        java.net.InetSocketAddress addr = player.getAddress();
        if (addr == null || addr.getAddress() == null)
            return;
            
        String currentIp = addr.getAddress().getHostAddress();

        if (lastKnownIp.containsKey(player.getUniqueId())) {
            String lastIp = lastKnownIp.get(player.getUniqueId());
            if (lastIp != null && !lastIp.equals(currentIp)) {
                plugin.getLogManager().logWarn("Security Warning: Player " + player.getName() + " IP changed from " + lastIp + " to " + currentIp);
                player.sendMessage(
                        ColorUtil.translate("&c&l[Security] &7Login detected from a new IP: &e" + currentIp));
                player.sendMessage(ColorUtil.translate("&7If this wasn't you, change your password immediately!"));
            }
        }

        lastKnownIp.put(player.getUniqueId(), currentIp);
    }

    public void recordFailedAttempt(String ip) {
        failedAttempts.put(ip, failedAttempts.getOrDefault(ip, 0) + 1);
    }

    public void resetFailedAttempts(String ip) {
        failedAttempts.remove(ip);
    }
}

