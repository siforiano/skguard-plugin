package com.soulguard.modules.connection;

import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import com.soulguard.util.ColorUtil;

public class WhitelistManager implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private final List<String> whitelistedNames;
    private boolean maintenanceMode;

    public WhitelistManager(SoulGuard plugin) {
        this.plugin = plugin;
        this.whitelistedNames = new java.util.ArrayList<>();
    }

    @Override
    public String getName() {
        return "WhitelistManager";
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
    }

    @Override
    public void reload() {
        this.maintenanceMode = plugin.getConfig().getBoolean("modules.WhitelistManager.maintenance", false);
        this.whitelistedNames.clear();
        List<String> players = plugin.getConfig().getStringList("modules.WhitelistManager.players");
        this.whitelistedNames.addAll(players);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        if (!enabled || !maintenanceMode)
            return;

        String name = event.getName();
        if (!whitelistedNames.contains(name)) {
            String kickMsg = plugin.getConfig().getString("modules.WhitelistManager.kick-message", "&cServer is in maintenance mode.");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                    ColorUtil.translate(kickMsg != null ? kickMsg : "&cServer is in maintenance mode."));
            plugin.getLogManager().logInfo("Prevented connection from " + name + " (Maintenance Active)");
        }
    }
}
