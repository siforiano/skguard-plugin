package com.skguard;

import com.skguard.database.SessionManager;

/**
 * Handles authentication at the Velocity Proxy level.
 */
public class ProxyAuthHandler {

    private final SKGuardVelocity plugin;

    public ProxyAuthHandler(SKGuardVelocity plugin, SessionManager sessionManager) {
        this.plugin = plugin;
        // SessionManager is reachable here if needed for future expansion
    }

    @com.velocitypowered.api.event.Subscribe
    public void onPostLogin(com.velocitypowered.api.event.connection.PostLoginEvent event) {
        com.velocitypowered.api.proxy.Player player = event.getPlayer();
        // Check if player is staff
        if (player.hasPermission("SKGuard.staff")) {
            plugin.getSlf4jLogger().info("Staff member " + player.getUsername() + " joined the network. Session tracking active.");
        }
    }
}

