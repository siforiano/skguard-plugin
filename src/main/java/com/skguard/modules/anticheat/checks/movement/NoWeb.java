package com.skguard.modules.anticheat.checks.movement;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.skguard.SKGuard;
import com.skguard.modules.anticheat.Check;
import com.skguard.modules.anticheat.PlayerData;

public class NoWeb extends Check {

    public NoWeb(SKGuard plugin) {
        super(plugin, "NoWeb", "movement");
    }

    @Override
    public void handleMove(org.bukkit.event.player.PlayerMoveEvent event, PlayerData data) {
        if (!isEnabled())
            return;

        Player player = event.getPlayer();
        if (player.hasPermission("SKGuard.bypass.anticheat"))
            return;

        // Check if player is currently in a cobweb
        Material type = player.getLocation().getBlock().getType();
        if (type != Material.COBWEB)
            return;

        org.bukkit.Location to = event.getTo();
        org.bukkit.Location from = event.getFrom();
        if (to == null || from == null)
            return;

        double deltaX = Math.abs(to.getX() - from.getX());
        double deltaZ = Math.abs(to.getZ() - from.getZ());
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        // Standard cobweb movement speed is very low approx 0.05 per tick
        // 0.15 is a safe threshold to prevent falses but catch most NoWeb hacks
        if (horizontalDistance > 0.15) {
            flag(player, "Illegal movement speed in cobweb: " + String.format("%.3f", horizontalDistance), 0.9);
            event.setCancelled(true); // Optimization: Active rubberband
        }
    }
}

