package com.soulguard.modules.premium.checks;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import com.soulguard.SoulGuard;
import com.soulguard.modules.anticheat.Check;

/**
 * Premium-only Scaffold check.
 * Detects automatic block placement (scaffolding hacks).
 */
public class ScaffoldCheck extends Check implements Listener {

    public ScaffoldCheck(SoulGuard plugin) {
        super(plugin, "Scaffold", "movement");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!isEnabled()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("soulguard.bypass.anticheat")) return;

        Block block = event.getBlockPlaced();
        
        // Check if player is placing blocks while moving fast
        if (player.getVelocity().length() > 0.3) {
            // Check if placing blocks below themselves (scaffold pattern)
            if (block.getY() < player.getLocation().getY() - 1) {
                flag(player, "Suspicious scaffold placement pattern", 0.75);
            }
        }
    }
}
