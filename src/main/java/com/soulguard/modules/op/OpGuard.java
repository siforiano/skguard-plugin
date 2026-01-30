package com.soulguard.modules.op;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class OpGuard implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private boolean alertOnly;

    public OpGuard(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "OpGuard";
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
        this.alertOnly = plugin.getConfig().getBoolean("modules.OpGuard.alert-only", false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!enabled)
            return;

        String msg = event.getMessage().toLowerCase();
        if (msg.startsWith("/op ") || msg.startsWith("/minecraft:op ")) {
            String issuer = event.getPlayer().getName();
            String target = msg.split(" ").length > 1 ? msg.split(" ")[1] : "unknown";

            plugin.getLogManager().logWarn("OP CHANGE DETECTED: " + issuer + " opped " + target);

            if (plugin.getDiscordWebhook() != null) {
                plugin.getDiscordWebhook().sendAlert("OP Change Detected",
                        "User **" + issuer + "** ran OP command on **" + target + "**", 16711680); // Red
            }

            if (!alertOnly && !event.getPlayer().hasPermission("soulguard.op.bypass")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "You are not authorized to OP players via command.");
                plugin.getLogManager().logWarn("Blocked unauthorized OP attempt by " + issuer);
            }
        }

        if (msg.startsWith("/deop ") || msg.startsWith("/minecraft:deop ")) {
            String issuer = event.getPlayer().getName();
            String target = msg.split(" ").length > 1 ? msg.split(" ")[1] : "unknown";

            plugin.getLogManager().logWarn("DEOP CHANGE: " + issuer + " deopped " + target);
            if (plugin.getDiscordWebhook() != null) {
                plugin.getDiscordWebhook().sendAlert("OP Change Detected",
                        "User **" + issuer + "** ran DEOP command on **" + target + "**", 16755200); // Orange
            }
        }
    }
}
