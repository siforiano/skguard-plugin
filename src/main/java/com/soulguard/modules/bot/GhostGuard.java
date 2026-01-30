package com.soulguard.modules.bot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import com.soulguard.util.ColorUtil;

public class GhostGuard implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private final Map<UUID, Long> suspiciousStrikes = new HashMap<>();

    private org.bukkit.scheduler.BukkitTask trapTask;

    public GhostGuard(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "GhostGuard";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        startTask();
    }

    @Override
    public void disable() {
        this.enabled = false;
        if (trapTask != null) {
            trapTask.cancel();
            trapTask = null;
        }
        suspiciousStrikes.clear();
    }

    private void startTask() {
        if (trapTask != null) return;
        trapTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!enabled) {
                    cancel();
                    trapTask = null;
                    return;
                }
                for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    spawnGhostTrap(p);
                }
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // Every 1 minute
    }

    @Override
    public void reload() {
        if (enabled) {
            if (trapTask == null) startTask();
        } else {
            if (trapTask != null) {
                trapTask.cancel();
                trapTask = null;
            }
        }
    }

    /**
     * Logic: Periodically spawn an invisible entity behind a player to check for
     * "Snap-head" or KillAura.
     */
    public void spawnGhostTrap(Player target) {
        if (!enabled)
            return;

        // 1. Randomly decide whether to spawn a ghost (behind) or a honeypot (in
        // front/side)
        if (ThreadLocalRandom.current().nextBoolean()) {
            spawnGhost(target);
        } else {
            spawnHoneypot(target);
        }
    }

    private void spawnGhost(Player target) {
        Location behind = target.getLocation().clone().add(target.getLocation().getDirection().multiply(-1.5));
        behind.setY(behind.getY() + 1.5);
        createEntity(target, behind, "SGGhost_");
    }

    private void spawnHoneypot(Player target) {
        // Spawn slightly in front but invisible to catch killaura that target nearest
        // entity
        Location front = target.getLocation().clone().add(target.getLocation().getDirection().multiply(2.5));
        front.setY(front.getY() + 1.0);
        createEntity(target, front, "SGHoney_");
    }

    private void createEntity(Player target, Location loc, String prefix) {
        org.bukkit.entity.ArmorStand stand = (org.bukkit.entity.ArmorStand) target.getWorld().spawnEntity(loc,
                EntityType.ARMOR_STAND);
        stand.setCustomName(prefix + UUID.randomUUID().toString().substring(0, 8));
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setSmall(true);
        stand.setVisible(false);
        stand.setMarker(true);
        stand.setGravity(false);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (stand.isValid())
                    stand.remove();
            }
        }.runTaskLater(plugin, 40L); // 2 seconds trap
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGhostAttack(EntityDamageByEntityEvent event) {
        if (!enabled || !(event.getDamager() instanceof Player))
            return;

        Entity victim = event.getEntity();
        String name = victim.getCustomName();
        if (name != null && (name.startsWith("SGGhost_") || name.startsWith("SGHoney_"))) {
            event.setCancelled(true);
            Player attacker = (Player) event.getDamager();

            long strikes = suspiciousStrikes.getOrDefault(attacker.getUniqueId(), 0L) + 1;
            suspiciousStrikes.put(attacker.getUniqueId(), strikes);

            String type = name.startsWith("SGGhost_") ? "Ghost" : "Honeypot";
            plugin.getLogManager().logWarn(
                    "GhostGuard [" + type + "]: " + attacker.getName() + " struck a trap! (Strike " + strikes + ")");

            if (strikes >= 3) {
                if (plugin.getInventorySnapshots().isEnabled()) {
                    plugin.getInventorySnapshots().takeSnapshot(attacker, "Bot Detection (" + type + ")");
                }
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        attacker.kickPlayer(ColorUtil
                                .translate("&c&l[SoulGuard] &7Advanced Bot detection triggered (" + type + ")"));
                    }
                }.runTask(plugin);
            }
        }
    }
}
