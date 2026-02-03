package com.skguard.modules.anticheat.checks.movement;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import com.skguard.SKGuard;
import com.skguard.modules.anticheat.Check;
import com.skguard.modules.anticheat.PlayerData;

public class SpeedA extends Check {

    private double maxHorizontalSpeed;

    public SpeedA(SKGuard plugin) {
        super(plugin, "SpeedA", "movement");
    }

    @Override
    public void reload() {
        super.reload();
        this.maxHorizontalSpeed = plugin.getConfig().getDouble("anticheat.checks.movement.SpeedA.max-speed", 0.65);
    }

    @Override
    public void handleMove(org.bukkit.event.player.PlayerMoveEvent event, PlayerData data) {
        if (!isEnabled())
            return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)
            return;
        if (player.hasPermission("SKGuard.bypass.anticheat"))
            return;

        org.bukkit.Location to = event.getTo();
        if (to == null)
            return;
        double deltaX = to.getX() - event.getFrom().getX();
        double deltaZ = to.getZ() - event.getFrom().getZ();
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        // Dynamic speed limit based on player speed attribute (simplified)
        double limit = maxHorizontalSpeed * (player.getWalkSpeed() / 0.2f);

        if (horizontalDistance > limit) {
            flag(player, "Horizontal speed exceed (Packet Delta)", 0.85);
        }
    }
}

