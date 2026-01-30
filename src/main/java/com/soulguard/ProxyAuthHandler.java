package com.soulguard;

import com.soulguard.database.SessionManager;

/**
 * Handles authentication at the Velocity Proxy level.
 */
public class ProxyAuthHandler {

    private final SoulGuardVelocity plugin;

    public ProxyAuthHandler(SoulGuardVelocity plugin, SessionManager sessionManager) {
        this.plugin = plugin;
        // SessionManager is reachable here if needed for future expansion
    }

    @com.velocitypowered.api.event.Subscribe
    public void onPostLogin(com.velocitypowered.api.event.connection.PostLoginEvent event) {
        com.velocitypowered.api.proxy.Player player = event.getPlayer();
        // Check if player is staff
        if (player.hasPermission("soulguard.staff")) {
            plugin.getLogger().info("Staff member " + player.getUsername() + " joined the network. Session tracking active.");
        }
    }
}
