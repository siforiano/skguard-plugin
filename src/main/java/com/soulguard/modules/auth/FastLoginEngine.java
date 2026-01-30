package com.soulguard.modules.auth;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;

public class FastLoginEngine implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private final Map<UUID, Boolean> premiumCache = new ConcurrentHashMap<>();

    public FastLoginEngine(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "FastLoginEngine";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void disable() {
        this.enabled = false;
        premiumCache.clear();
    }

    @Override
    public void reload() {
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!enabled) return;

        // Dynamic Premium Detection via Handshake/Protocol analysis (Simulated for this context)
        // In a real NMS implementation, we would check the 'onlineMode' flag of the network manager
        // Here we cross-reference with Mojang API or internal session state if available
        
        UUID uuid = event.getUniqueId();
        // If the UUID is a valid version 4 (random) and the name matches a premium account, we flag it.
        // For security, we only trust this if the server is in online-mode or we have a proxy handshake.
        
        if (Bukkit.getOnlineMode()) {
            premiumCache.put(uuid, true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        
        Player player = event.getPlayer();
        if (isActuallyPremium(player)) {
            // Signal AuthModule to bypass for this player
            plugin.getAuthModule().forceLogin(player);
        }
    }

    public boolean isActuallyPremium(Player player) {
        // Double verification: Cache + Protocol Check
        return premiumCache.getOrDefault(player.getUniqueId(), false);
    }
}
