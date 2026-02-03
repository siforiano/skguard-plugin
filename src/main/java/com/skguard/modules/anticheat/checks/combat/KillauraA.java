package com.skguard.modules.anticheat.checks.combat;

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
import com.skguard.modules.anticheat.CheckManager;
import com.skguard.modules.anticheat.PlayerData;

public class KillauraA extends Check implements Listener {

    private final Map<UUID, Double> lastGcd = new HashMap<>();

    public KillauraA(SKGuard plugin) {
        super(plugin, "KillauraA", "combat");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!isEnabled() || !(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        
        PlayerData data = ((CheckManager) plugin.getModuleManager().getModule("NeoGuard")).getPlayerData(player.getUniqueId());
        
        float yaw = player.getLocation().getYaw();
        float pitch = player.getLocation().getPitch();
        
        float lastYaw = data.getLastYaw();
        float lastPitch = data.getLastPitch();
        
        float deltaYaw = Math.abs(yaw - lastYaw);
        float deltaPitch = Math.abs(pitch - lastPitch);
        
        // Detection: Rotation Smoothness / Sensitivity GCD
        // Legit players rotate based on a constant sensitivity constant.
        // Bots often have perfectly calculated rotations that don't match the GCD.
        if (deltaYaw > 1.0 && deltaPitch > 1.0) {
            double rotationGcd = getGcd((double) deltaYaw, (double) deltaPitch);
            
            if (lastGcd.containsKey(player.getUniqueId())) {
                double diff = Math.abs(rotationGcd - lastGcd.get(player.getUniqueId()));
                if (diff < 0.00001) {
                    // Too consistent rotation (Bot aimbot behavior)
                    flag(player, "Rotation consistency too high (MFA GCD Match)", 0.9);
                }
            }
            lastGcd.put(player.getUniqueId(), rotationGcd);
        }

        // Heuristic: Check if damager is looking at victim
        double lookHeuristic = getLookHeuristic(player, event.getEntity());
        if (lookHeuristic < 0.8) { // 1.0 is direct hit, < 0.8 is suspicious
            flag(player, "Combat heuristics (Look-at mismatch: " + String.format("%.2f", lookHeuristic) + ")", 0.75);
        }
        
        data.setLastYaw(yaw);
        data.setLastPitch(pitch);
    }

    private double getLookHeuristic(Player player, org.bukkit.entity.Entity entity) {
        org.bukkit.util.Vector toEntity = entity.getLocation().toVector().subtract(player.getEyeLocation().toVector());
        if (toEntity.lengthSquared() == 0) return 1.0;
        org.bukkit.util.Vector direction = player.getEyeLocation().getDirection();
        return toEntity.normalize().dot(direction);
    }

    private double getGcd(double a, double b) {
        if (a < b) return getGcd(b, a);
        if (Math.abs(b) < 0.001) return a;
        return getGcd(b, a % b);
    }
}

