package com.skguard.modules.premium.checks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;

import com.skguard.SKGuard;
import com.skguard.modules.anticheat.Check;

/**
 * Premium-only FastBow check.
 * Detects players shooting arrows faster than possible.
 */
public class FastBowCheck extends Check implements Listener {

    private final Map<UUID, Long> lastShootTime;

    public FastBowCheck(SKGuard plugin) {
        super(plugin, "FastBow", "combat");
        this.lastShootTime = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!isEnabled() || !(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        if (player.hasPermission("SKGuard.bypass.anticheat")) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastShootTime.get(uuid);

        if (last != null) {
            long delta = now - last;
            // Minimum bow charge time is ~300ms
            if (delta < 250) {
                flag(player, "FastBow detected (shot too quickly)", 0.85);
            }
        }

        lastShootTime.put(uuid, now);
    }
}

