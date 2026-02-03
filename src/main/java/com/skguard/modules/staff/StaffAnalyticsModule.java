package com.skguard.modules.staff;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;

public class StaffAnalyticsModule implements SecurityModule {

    private boolean enabled;
    private final Map<UUID, StaffStats> stats = new HashMap<>();

    public StaffAnalyticsModule(SKGuard plugin) {
    }

    @Override
    public String getName() {
        return "StaffAnalytics";
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
        stats.clear();
    }

    @Override
    public void reload() {
    }

    public void logAction(Player staff) {
        if (!enabled)
            return;
        StaffStats s = stats.computeIfAbsent(staff.getUniqueId(), k -> new StaffStats());
        s.actions++;
        s.lastSeen = System.currentTimeMillis();
        
        // Heatmap: count per hour (0-23)
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        s.hourlyActivity[hour]++;
    }

    public StaffStats getStats(UUID uuid) {
        return stats.getOrDefault(uuid, new StaffStats());
    }

    public static class StaffStats {
        public int actions = 0;
        public int bans = 0;
        public int reportsResolved = 0;
        public long lastSeen = 0;
        public int[] hourlyActivity = new int[24];
    }
}

