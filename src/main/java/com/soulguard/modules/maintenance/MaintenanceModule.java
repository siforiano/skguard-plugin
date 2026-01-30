package com.soulguard.modules.maintenance;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import com.soulguard.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class MaintenanceModule implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private boolean maintenanceActive;
    private String maintenanceMessage;

    public MaintenanceModule(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Maintenance";
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
        this.maintenanceActive = false;
    }

    @Override
    public void reload() {
        this.maintenanceActive = plugin.getConfig().getBoolean("modules.Maintenance.active", false);
        this.maintenanceMessage = plugin.getConfig().getString("modules.Maintenance.message",
                "&e&lMAINTENANCE\n&7We will be back soon!");
    }

    public boolean isMaintenanceActive() {
        return maintenanceActive;
    }

    public void setMaintenanceActive(boolean active) {
        this.maintenanceActive = active;
        plugin.getConfig().set("modules.Maintenance.active", active);
        plugin.saveConfig();

        if (active) {
            kickNonStaff();
        }
    }

    private void kickNonStaff() {
        String msg = ColorUtil.translate(maintenanceMessage);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("soulguard.staff") && !player.isOp()) {
                player.kickPlayer(msg);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        if (enabled && maintenanceActive) {
            // Note: Permissions cannot be checked here.
            // Usually maintenance is handled by checking a whitelist of UUIDs or names.
            // For a simple implementation, we can allow players who were already staff
            // if we cache them, or just block everyone and rely on OP/Console to toggle.
            // Better: Check if the player name/UUID is in a staff list.
            // For now, we disallow and recommend staff join when active is false or
            // implement a better staff check if requested.
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, ColorUtil.translate(maintenanceMessage));
        }
    }
}
