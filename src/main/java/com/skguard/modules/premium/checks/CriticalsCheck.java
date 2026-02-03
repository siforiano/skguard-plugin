package com.skguard.modules.premium.checks;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.skguard.SKGuard;
import com.skguard.modules.anticheat.Check;

/**
 * Premium-only Criticals check.
 * Detects fake critical hits.
 */
public class CriticalsCheck extends Check implements Listener {

    public CriticalsCheck(SKGuard plugin) {
        super(plugin, "Criticals", "combat");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!isEnabled() || !(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        if (player.hasPermission("SKGuard.bypass.anticheat")) return;

        // Critical hits require falling
        @SuppressWarnings("deprecation")
        boolean onGround = player.isOnGround();
        double fallDistance = player.getFallDistance();

        if (onGround && fallDistance < 0.1) {
            // Player is on ground but dealing critical damage
            flag(player, "Fake critical hit detected", 0.8);
        }
    }
}

