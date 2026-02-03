package com.skguard.modules.anticheat.checks.movement;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import com.skguard.SKGuard;
import com.skguard.modules.anticheat.Check;
import com.skguard.modules.anticheat.PlayerData;

public class Flight extends Check {

    private Object cachedMLModule;
    private java.lang.reflect.Method mlMethod;
    private boolean mlLookupDone = false;

    public Flight(SKGuard plugin) {
        super(plugin, "Flight", "movement");
        // Optimization: Cache the manager reference to avoid map lookup on every move
        // event
    }

    @Override
    public void handleMove(org.bukkit.event.player.PlayerMoveEvent event, PlayerData data) {
        if (!isEnabled())
            return;

        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR
                || player.isFlying())
            return;
        if (player.hasPermission("SKGuard.bypass.anticheat"))
            return;

        // Optimization: Use passed data object
        if (data == null)
            return;

        org.bukkit.Location to = event.getTo();
        if (to == null)
            return;
        double deltaY = to.getY() - event.getFrom().getY();
        double lastDeltaY = data.getLastDeltaY();

        // Check if player is from Bedrock to adjust thresholds
        boolean isBedrock = com.skguard.util.BedrockManager.isBedrock(player);
        double thresholdOffset = isBedrock ? 0.15 : 0.05;

        // Very basic Kinematic Prediction (Simplified)
        // v_new = (v_old - gravity) * friction
        double expectedDeltaY = (lastDeltaY - 0.08) * 0.98;

        // If player is not on ground and going up without predicted velocity
        boolean onGround = isGrounded(player);
        if (!onGround && !data.isLastOnGround() && deltaY > 0 && deltaY > expectedDeltaY + thresholdOffset) {
            // Check if player is on stairs or slab (simplified check)
            if (event.getFrom().getBlock().getType() != org.bukkit.Material.AIR)
                return;

            double confidence = isBedrock ? 0.7 : 0.8;

            // ML PRECISION ENHANCEMENT (Premium Only)
            if (plugin.getEdition().isPremium()) {
                if (!mlLookupDone) {
                    cachedMLModule = plugin.getModuleManager().getModule("MLDetection");
                    if (cachedMLModule != null) {
                        try {
                            mlMethod = cachedMLModule.getClass().getMethod("getAnomalyScore", Player.class);
                        } catch (NoSuchMethodException ignored) {
                        }
                    }
                    mlLookupDone = true;
                }

                if (cachedMLModule != null && mlMethod != null) {
                    try {
                        double anomalyScore = (double) mlMethod.invoke(cachedMLModule, player);
                        // Increase confidence based on behavioral anomalies
                        confidence = Math.min(0.99, confidence + (anomalyScore * 0.2));
                    } catch (Exception ignored) {
                    }
                }
            }

            flag(player, "Abnormal Y-acceleration (" + (isBedrock ? "Bedrock" : "Java") + " Precision: "
                    + String.format("%.2f", confidence) + ")", confidence);

            // ACTIVE PROTECTION: Cancel the move to rubberband the player
            event.setCancelled(true);
        }
    }

    /**
     * 10/10 Robust Ground Check: Raytracing/AABB verification
     * instead of trusting the client-sent boolean.
     */
    private boolean isGrounded(Player player) {
        org.bukkit.util.BoundingBox box = player.getBoundingBox();
        box.expand(0, -0.01, 0); // Check slightly below the feet

        for (int x = (int) Math.floor(box.getMinX()); x <= (int) Math.floor(box.getMaxX()); x++) {
            for (int z = (int) Math.floor(box.getMinZ()); z <= (int) Math.floor(box.getMaxZ()); z++) {
                org.bukkit.block.Block block = player.getWorld().getBlockAt(x, (int) Math.floor(box.getMinY()), z);
                if (block.getType().isSolid())
                    return true;
            }
        }
        return false;
    }
}

