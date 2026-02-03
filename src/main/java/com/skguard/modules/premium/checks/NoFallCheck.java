package com.skguard.modules.premium.checks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import com.skguard.SKGuard;
import com.skguard.modules.anticheat.Check;

/**
 * Premium-only NoFall check.
 * Detects players canceling fall damage.
 */
public class NoFallCheck extends Check implements Listener {

    private final Map<UUID, Double> lastY;

    public NoFallCheck(SKGuard plugin) {
        super(plugin, "NoFall", "movement");
        this.lastY = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!isEnabled()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("SKGuard.bypass.anticheat")) return;

        org.bukkit.Location to = event.getTo();
        if (to == null) return;

        UUID uuid = player.getUniqueId();
        Double last = lastY.get(uuid);

        if (last != null) {
            double fallDistance = last - to.getY();
            
            // Player fell significantly but has no fall distance recorded
            if (fallDistance > 3 && player.getFallDistance() < 0.5) {
                @SuppressWarnings("deprecation")
                boolean onGround = player.isOnGround();
                if (!onGround) {
                    flag(player, "NoFall detected (fall distance manipulation)", 0.85);
                }
            }
        }

        lastY.put(uuid, to.getY());
    }
}

