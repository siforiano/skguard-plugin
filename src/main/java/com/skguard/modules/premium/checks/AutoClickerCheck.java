package com.skguard.modules.premium.checks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.skguard.SKGuard;
import com.skguard.modules.anticheat.Check;

/**
 * Premium-only AutoClicker detection check.
 * <p>
 * Uses statistical analysis of click patterns to detect auto-clickers.
 * Analyzes CPS (Clicks Per Second), consistency, and timing patterns.
 * </p>
 * 
 * <p><b>Premium Feature</b> - Only available in SKGuard Premium</p>
 * 
 * @author SKGuard Team
 * @since 1.0
 */
public class AutoClickerCheck extends Check implements Listener {

    private final Map<UUID, ClickData> clickData;

    public AutoClickerCheck(SKGuard plugin) {
        super(plugin, "AutoClicker", "combat");
        this.clickData = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!isEnabled()) return;
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (player.hasPermission("SKGuard.bypass.anticheat")) return;

        UUID uuid = player.getUniqueId();
        ClickData data = clickData.computeIfAbsent(uuid, k -> new ClickData());

        long now = System.currentTimeMillis();
        data.addClick(now);

        // Analyze after collecting enough data
        if (data.getClickCount() >= 20) {
            double cps = data.getCPS();
            double consistency = data.getConsistency();

            // Flag if CPS is too high or too consistent (bot-like)
            if (cps > 20) {
                flag(player, "Suspicious CPS: " + String.format("%.1f", cps), 0.8);
            } else if (consistency > 0.95 && cps > 10) {
                flag(player, "Bot-like click consistency: " + String.format("%.2f", consistency), 0.9);
            }
        }
    }

    private static class ClickData {
        private final long[] clickTimes = new long[20];
        private int index = 0;
        private int count = 0;

        public void addClick(long time) {
            clickTimes[index] = time;
            index = (index + 1) % clickTimes.length;
            if (count < clickTimes.length) count++;
        }

        public int getClickCount() {
            return count;
        }

        public double getCPS() {
            if (count < 2) return 0;
            long oldest = clickTimes[(index + clickTimes.length - count) % clickTimes.length];
            long newest = clickTimes[(index + clickTimes.length - 1) % clickTimes.length];
            double duration = (newest - oldest) / 1000.0;
            return duration > 0 ? count / duration : 0;
        }

        public double getConsistency() {
            if (count < 3) return 0;
            
            // Calculate standard deviation of intervals
            double[] intervals = new double[count - 1];
            for (int i = 0; i < count - 1; i++) {
                int idx1 = (index + clickTimes.length - count + i) % clickTimes.length;
                int idx2 = (index + clickTimes.length - count + i + 1) % clickTimes.length;
                intervals[i] = clickTimes[idx2] - clickTimes[idx1];
            }

            double mean = 0;
            for (double interval : intervals) mean += interval;
            mean /= intervals.length;

            double variance = 0;
            for (double interval : intervals) {
                variance += Math.pow(interval - mean, 2);
            }
            variance /= intervals.length;

            double stdDev = Math.sqrt(variance);
            return mean > 0 ? 1.0 - (stdDev / mean) : 0;
        }
    }
}

