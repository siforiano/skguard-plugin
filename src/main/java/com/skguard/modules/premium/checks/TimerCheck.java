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
 * Premium-only Timer check.
 * Detects players speeding up their game time.
 */
public class TimerCheck extends Check implements Listener {

    private final Map<UUID, Long> lastMoveTime;
    private final Map<UUID, Integer> timerViolations;

    public TimerCheck(SKGuard plugin) {
        super(plugin, "Timer", "movement");
        this.lastMoveTime = new HashMap<>();
        this.timerViolations = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!isEnabled()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("SKGuard.bypass.anticheat")) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastMoveTime.get(uuid);

        if (last != null) {
            long delta = now - last;
            // Normal tick is ~50ms, flag if consistently faster
            if (delta < 30) {
                int violations = timerViolations.getOrDefault(uuid, 0) + 1;
                timerViolations.put(uuid, violations);
                if (violations > 10) {
                    flag(player, "Timer detected (game speed modification)", 0.9);
                    timerViolations.put(uuid, 0);
                }
            } else {
                timerViolations.put(uuid, Math.max(0, timerViolations.getOrDefault(uuid, 0) - 1));
            }
        }

        lastMoveTime.put(uuid, now);
    }
}

