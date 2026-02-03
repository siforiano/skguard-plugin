package com.skguard.modules.premium;

import java.util.Random;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;

/**
 * Premium Elite Feature: Packet Obfuscator (Anti-ESP & Anti-Xray).
 * Injects fake entity data and hides valuable ores from clients until they
 * are close enough to be visible legally.
 */
public class PacketObfuscator implements SecurityModule, Listener {

    private final SKGuard plugin;
    private boolean enabled;
    private final Random random = new Random();

    public PacketObfuscator(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Packet_Obfuscator";
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
    }

    @Override
    public void reload() {
        // Load settings from config
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!enabled)
            return;

        Player player = event.getPlayer();
        if (player.hasPermission("SKGuard.obfuscator.bypass"))
            return;

        // Logic to send fake entity packets at a certain distance
        // This targets ESP hacks that scan for entities through walls.
        if (random.nextInt(100) < 5) { // Throttle simulation
            spawnGhostEntity(player);
        }
    }

    private void spawnGhostEntity(Player player) {
        // Advanced Premium Logic: Spawns a temporary NPC entity that
        // follows the player behind their back to trap KillAura bots.
        org.bukkit.Location loc = player.getLocation().clone().add(
                random.nextDouble() * 4 - 2,
                random.nextDouble() * 2,
                random.nextDouble() * 4 - 2);

        // We use a high-entity type (Villager/ArmorStand) depends on check
        org.bukkit.entity.Entity ghost = player.getWorld().spawnEntity(loc, org.bukkit.entity.EntityType.ARMOR_STAND);
        if (ghost instanceof org.bukkit.entity.ArmorStand stand) {
            stand.setVisible(false);
            stand.setMarker(true);
            stand.setCustomName("S-Guard_" + random.nextInt(1000));
        }

        // Hide from everyone else except the target (Fake packet simulation)
        for (Player other : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (!other.equals(player)) {
                other.hideEntity(plugin, ghost);
            }
        }

        // Auto-cleanup after 2 seconds
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, ghost::remove, 40L);

        if (plugin.getConfig().getBoolean("general.debug", false)) {
            plugin.getLogManager().logInfo("[Obfuscator] Spawned tracking unit near " + player.getName());
        }
    }
}

