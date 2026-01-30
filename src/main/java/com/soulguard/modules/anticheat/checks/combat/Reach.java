package com.soulguard.modules.anticheat.checks.combat;

import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.soulguard.SoulGuard;
import com.soulguard.modules.anticheat.Check;

public class Reach extends Check implements Listener {

    public Reach(SoulGuard plugin) {
        super(plugin, "Reach", "combat");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!isEnabled()) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        Entity victim = event.getEntity();
        if (attacker.getGameMode() == GameMode.CREATIVE) return;
        if (attacker.hasPermission("soulguard.bypass.anticheat")) return;

        double distance = attacker.getEyeLocation().distance(victim.getLocation());
        
        // Simple hitbox compensation (approximate)
        // Static threshold for simplicity in this version, usually 3.3 - 3.5 is the limit for survival
        double threshold = 3.6;

        if (distance > threshold) {
            flag(attacker, "Reach too high: " + String.format("%.2f", distance) + " blocks", 0.9);
            event.setCancelled(true);
        }
    }
}
