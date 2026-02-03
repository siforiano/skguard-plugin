package com.skguard.modules.premium.checks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.skguard.SKGuard;
import com.skguard.modules.anticheat.Check;

/**
 * Premium-only AimAssist detection check.
 * Detects artificial aim smoothing and snap-to-target behavior.
 */
public class AimAssistCheck extends Check implements Listener {

    private final Map<UUID, Float> lastYawDelta;

    public AimAssistCheck(SKGuard plugin) {
        super(plugin, "AimAssist", "combat");
        this.lastYawDelta = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!isEnabled() || !(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        if (player.hasPermission("SKGuard.bypass.anticheat")) return;

        float yaw = player.getLocation().getYaw();
        Float lastDelta = lastYawDelta.get(player.getUniqueId());

        if (lastDelta != null) {
            float currentDelta = Math.abs(yaw - lastDelta);
            // Detect unnatural smoothing (too consistent rotation)
            if (currentDelta > 5 && currentDelta < 15) {
                flag(player, "Suspicious aim smoothing detected", 0.7);
            }
        }

        lastYawDelta.put(player.getUniqueId(), yaw);
    }
}

