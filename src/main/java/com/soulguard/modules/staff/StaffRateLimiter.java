package com.soulguard.modules.staff;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import com.soulguard.util.ColorUtil;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StaffRateLimiter implements SecurityModule {

    private final SoulGuard plugin;
    private boolean enabled;
    private final Map<UUID, ActionRecord> staffActions = new HashMap<>();
    private int maxActionsPerMinute = 5;

    public StaffRateLimiter(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "StaffRateLimit";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        this.maxActionsPerMinute = plugin.getConfig().getInt("modules.StaffRateLimit.max-per-minute", 5);
    }

    @Override
    public void disable() {
        this.enabled = false;
        staffActions.clear();
    }

    @Override
    public void reload() {
        enable();
    }

    public boolean canPerformAction(Player staff) {
        if (!enabled || staff.hasPermission("soulguard.bypass.ratelimit"))
            return true;

        long now = System.currentTimeMillis();
        ActionRecord record = staffActions.computeIfAbsent(staff.getUniqueId(), k -> new ActionRecord());

        // Reset if minute passed
        if (now - record.lastReset > 60000) {
            record.count = 0;
            record.lastReset = now;
        }

        record.count++;

        if (record.count > maxActionsPerMinute) {
            staff.sendMessage(ColorUtil.translate(
                    "&c&l[Security] &7Rate limit exceeded! You are performing too many administrative actions."));
            plugin.getLogManager().logWarn("Staff " + staff.getName() + " reached rate limit for admin actions.");
            return false;
        }

        return true;
    }

    private static class ActionRecord {
        int count = 0;
        long lastReset = System.currentTimeMillis();
    }
}
