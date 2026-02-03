package com.skguard.modules.punish;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;
import com.skguard.util.ColorUtil;

public class AutoPunishModule implements SecurityModule {

    private final SKGuard plugin;
    private boolean enabled;
    private final Map<UUID, Integer> violationPoints = new HashMap<>();
    private int threshold;

    public AutoPunishModule(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "AutoPunish";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        this.threshold = plugin.getConfig().getInt("modules.AutoPunish.threshold", 10);
    }

    @Override
    public void disable() {
        this.enabled = false;
    }

    @Override
    public void reload() {
        enable();
    }

    public void addViolation(Player player, int points, String reason) {
        if (!enabled)
            return;

        UUID uuid = player.getUniqueId();
        int current = violationPoints.getOrDefault(uuid, 0) + points;
        violationPoints.put(uuid, current);

        plugin.getLogManager()
                .logWarn("Violation added to " + player.getName() + " (" + current + "/" + threshold + "): " + reason);
        plugin.getAuditManager().logAction(player, "VIOLATION_ADDED", points + " points for " + reason);

        if (current >= threshold) {
            executePunishment(player, reason);
            violationPoints.remove(uuid);
        }
    }

    private void executePunishment(Player player, String reason) {
        plugin.getLogManager().logWarn("AUTO-PUNISHING " + player.getName() + " for exceeding violation threshold!");

        // Final punishment: Auto-Ban (simulated for now or real if command is known)
        String banMsg = ColorUtil.translate("&c[SKGuard] Auto-Punish: High violation score.\n&7Reason: " + reason);

        // Execute on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.kickPlayer(banMsg);
            // In a real scenario, we would run a ban command here:
            // Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban " + player.getName() +
            // " [SKGuard] Systematic violations.");
        });

        plugin.getAuditManager().logAction(player, "AUTO_PUNISH_EXECUTED", "Kicked for systematic violations.");
    }
}

