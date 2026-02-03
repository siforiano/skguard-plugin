package com.skguard.modules.staff;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;
import com.skguard.util.ColorUtil;

public class CommandFirewallModule implements SecurityModule, Listener {

    private final SKGuard plugin;
    private boolean enabled;
    private final List<String> restrictedCommands = Arrays.asList("/ban", "/pardon", "/kick", "/tempban", "/ipban",
            "/stop", "/reload", "/op", "/deop");

    public CommandFirewallModule(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "CommandFirewall";
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
    }

    @Override
    public void reload() {
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!enabled)
            return;

        Player player = event.getPlayer();
        String cmd = event.getMessage().toLowerCase().split(" ")[0];

        if (restrictedCommands.contains(cmd)) {
            // Check if player is staff
            if (player.hasPermission("SKGuard.staff")) {
                // Check verification status
                boolean verified = true;
                if (plugin.getStaffPIN().isEnabled() && plugin.getStaffPIN().isPending(player.getUniqueId())) {
                    verified = false;
                }
                if (plugin.getDiscordLink().isEnabled() && !plugin.getDiscordLink().isVerified(player.getUniqueId())) {
                    verified = false;
                }

                if (!verified) {
                    event.setCancelled(true);
                    if (plugin.getInventorySnapshots().isEnabled()) {
                        plugin.getInventorySnapshots().takeSnapshot(player, "Firewall Block (Unverified Staff)");
                    }
                    player.sendMessage(
                            ColorUtil.translate("&c&l[SKGuard] &7Command Firewall blocked: Complete MFA first!"));
                    plugin.getLogManager()
                            .logWarn("Staff " + player.getName() + " blocked by Firewall: Unverified MFA.");
                }
            }
        }
    }
}

