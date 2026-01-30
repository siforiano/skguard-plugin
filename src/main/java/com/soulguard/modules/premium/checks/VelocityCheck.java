package com.soulguard.modules.premium.checks;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import com.soulguard.SoulGuard;
import com.soulguard.modules.anticheat.Check;

/**
 * Premium-only Velocity check.
 * Detects players modifying their knockback velocity.
 */
public class VelocityCheck extends Check implements Listener {

    public VelocityCheck(SoulGuard plugin) {
        super(plugin, "Velocity", "combat");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!isEnabled() || !(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (player.hasPermission("soulguard.bypass.anticheat")) return;

        Vector velocity = player.getVelocity();
        double velocityMagnitude = velocity.length();

        // Check if velocity is suspiciously low after taking damage
        if (velocityMagnitude < 0.1) {
            flag(player, "Suspicious velocity modification", 0.8);
        }
    }
}
