package com.soulguard.modules.anticheat.checks.movement;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.soulguard.SoulGuard;
import com.soulguard.modules.anticheat.Check;
import com.soulguard.modules.anticheat.PlayerData;

public class LiquidWalk extends Check {

    public LiquidWalk(SoulGuard plugin) {
        super(plugin, "LiquidWalk", "movement");
    }

    @Override
    public void handleMove(org.bukkit.event.player.PlayerMoveEvent event, PlayerData data) {
        if (!isEnabled())
            return;

        Player player = event.getPlayer();
        if (player.hasPermission("soulguard.bypass.anticheat"))
            return;

        Material blockType = player.getLocation().getBlock().getType();
        Material belowType = player.getLocation().clone().subtract(0, 0.1, 0).getBlock().getType();

        if ((blockType == Material.WATER || blockType == Material.LAVA) &&
                player.getLocation().getY() % 1.0 > 0.9 && !player.isFlying()) {

            // Checking if player is actually standing on liquid surface
            if (belowType == Material.WATER || belowType == Material.LAVA) {
                flag(player, "Walking on liquid surface (Jesus)", 0.95);
            }
        }
    }
}
