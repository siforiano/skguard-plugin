package com.skguard.modules.ratelimit;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;

public class RateLimiter implements SecurityModule, Listener {

    private final SKGuard plugin;
    private boolean enabled;
    private final Map<UUID, ActionTracker> trackers;

    private int maxActionsPerSecond;
    private int violationThreshold;

    private org.bukkit.scheduler.BukkitTask cleanupTask;

    public RateLimiter(SKGuard plugin) {
        this.plugin = plugin;
        this.trackers = new ConcurrentHashMap<>();
    }

    @Override
    public String getName() {
        return "RateLimiter";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        startCleanupTask();
        reload();
    }

    @Override
    public void disable() {
        this.enabled = false;
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        trackers.clear();
    }

    private void startCleanupTask() {
        if (cleanupTask != null) return;
        cleanupTask = new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                // Clear trackers for offline players to save memory
                trackers.keySet().removeIf(uuid -> org.bukkit.Bukkit.getPlayer(uuid) == null);
            }
        }.runTaskTimer(plugin, 6000L, 6000L); // Every 5 minutes
    }

    @Override
    public void reload() {
        this.maxActionsPerSecond = plugin.getConfig().getInt("modules.RateLimiter.max-actions-per-second", 20);
        this.violationThreshold = plugin.getConfig().getInt("modules.RateLimiter.violation-threshold", 3);
    }
    
    // Cleanup on player quit
    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        trackers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!enabled)
            return;
        if (checkRateLimit(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!enabled)
            return;
        if (checkRateLimit(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        if (!enabled)
            return;
        if (checkRateLimit(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!enabled)
            return;
        if (checkRateLimit(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getLanguageManager().getMessage("modules.rate-limit"));
        }
    }

    private boolean checkRateLimit(Player player) {
        if (player.hasPermission("SKGuard.bypass.ratelimit"))
            return false;

        UUID uuid = player.getUniqueId();
        ActionTracker tracker = trackers.computeIfAbsent(uuid, k -> new ActionTracker());

        long now = System.currentTimeMillis();
        tracker.addAction(now);

        int actionsInLastSecond = tracker.getActionsInLastSecond(now);

        if (actionsInLastSecond > maxActionsPerSecond) {
            tracker.violations++;

            if (tracker.violations >= violationThreshold) {
                player.kickPlayer(plugin.getLanguageManager().getMessage("modules.rate-limit"));
                plugin.getLogManager().logWarn("Player " + player.getName() + " kicked for rate limit violations");

                if (plugin.getDiscordWebhook() != null) {
                    plugin.getDiscordWebhook().sendAlert("Rate Limit Violation",
                            "Player **" + player.getName() + "** kicked for excessive actions (" + actionsInLastSecond
                                    + "/s)",
                            16755200);
                }
            }

            return true;
        }

        return false;
    }

    private static class ActionTracker {
        private final Queue<Long> actions = new LinkedBlockingQueue<>();
        private int violations = 0;

        void addAction(long timestamp) {
            actions.offer(timestamp);
            if (actions.size() > 100) {
                actions.poll();
            }
        }

        int getActionsInLastSecond(long now) {
            actions.removeIf(time -> now - time > 1000);
            return actions.size();
        }
    }
}

