package com.skguard.modules.bot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;

public class BehaviorMonitor implements SecurityModule, Listener {

    private final SKGuard plugin;
    private boolean enabled;
    private final Map<UUID, PlayerMetrics> metrics = new ConcurrentHashMap<>();

    public BehaviorMonitor(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "BehaviorMonitor";
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
        metrics.clear();
    }

    @Override
    public void reload() {
    }

    private final Map<String, List<Long>> ipJoins = new ConcurrentHashMap<>();

    public int getRiskScore(Player player) {
        if (!enabled) return 0;
        int score = 0;
        
        // IP Join Frequency (e.g. > 3 joins in 10 seconds)
        java.net.InetSocketAddress addr = player.getAddress();
        if (addr != null && addr.getAddress() != null) {
            String ip = addr.getAddress().getHostAddress();
            List<Long> joins = ipJoins.getOrDefault(ip, new ArrayList<>());
            long now = System.currentTimeMillis();
            joins.removeIf(t -> (now - t) > 10000);
            if (joins.size() > 3) score += 40;
        }

        // Name heuristics (very simple: mostly numbers or very short)
        String name = player.getName();
        if (name.length() < 4) score += 20;
        int digitCount = 0;
        for (char c : name.toCharArray()) if (Character.isDigit(c)) digitCount++;
        if (digitCount > name.length() / 2) score += 30;

        return score;
    }

    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        if (!enabled) return;
        java.net.InetSocketAddress addr = event.getPlayer().getAddress();
        if (addr != null && addr.getAddress() != null) {
            String ip = addr.getAddress().getHostAddress();
            List<Long> joins = ipJoins.computeIfAbsent(ip, k -> new ArrayList<>());
            long now = System.currentTimeMillis();
            joins.add(now);
            
            // Optimization: Keep map clean
            if (ipJoins.size() > 500) {
                cleanupIpJoins();
            }
        }
    }

    private synchronized void cleanupIpJoins() {
        long now = System.currentTimeMillis();
        ipJoins.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(t -> (now - t) > 10000);
            return entry.getValue().isEmpty();
        });
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        metrics.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!enabled)
            return;

        Material type = event.getBlock().getType();
        if (type == Material.DIAMOND_ORE || type == Material.DEEPSLATE_DIAMOND_ORE) {
            UUID uuid = event.getPlayer().getUniqueId();
            PlayerMetrics m = metrics.computeIfAbsent(uuid, k -> new PlayerMetrics());
            m.recordDiamond();

            if (m.isAtypicalMining()) {
                plugin.getLogManager().logWarn("[Behavior] Atypical mining pattern detected for "
                        + event.getPlayer().getName() + " (Efficiency too high)");
            }
        }
    }

    private static class PlayerMetrics {
        private final List<Long> diamondTimes = new ArrayList<>();

        void recordDiamond() {
            diamondTimes.add(System.currentTimeMillis());
            if (diamondTimes.size() > 20)
                diamondTimes.remove(0);
        }

        boolean isAtypicalMining() {
            if (diamondTimes.size() < 10)
                return false;

            // If they mined 10 diamonds in less than 2 minutes (straight line mining?)
            long first = diamondTimes.get(0);
            long last = diamondTimes.get(diamondTimes.size() - 1);
            return (last - first) < 120000;
        }
    }
}

