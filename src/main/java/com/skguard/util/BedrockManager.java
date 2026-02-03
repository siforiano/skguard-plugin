package com.skguard.util;

import java.util.UUID;

import org.bukkit.entity.Player;

/**
 * Utility class for handling Bedrock (Geyser/Floodgate) player detection.
 * Essential for platform-specific anti-cheat thresholds and UI delivery.
 * 
 * @author SKGuard Team
 * @since 1.0 (Ultimate Edition)
 */
public class BedrockManager {

    private static boolean floodgatePresent = false;

    static {
        try {
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            floodgatePresent = true;
        } catch (ClassNotFoundException e) {
            floodgatePresent = false;
        }
    }

    /**
     * Checks if a player is from Bedrock Edition.
     * 
     * @param player the player to check
     * @return true if the player is connected via Geyser/Floodgate
     */
    public static boolean isBedrock(Player player) {
        if (!floodgatePresent) return false;
        
        try {
            return org.geysermc.floodgate.api.FloodgateApi.getInstance()
                    .isFloodgatePlayer(player.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if a player is from Bedrock Edition by UUID.
     */
    public static boolean isBedrock(UUID uuid) {
        if (!floodgatePresent) return false;
        
        try {
            return org.geysermc.floodgate.api.FloodgateApi.getInstance()
                    .isFloodgatePlayer(uuid);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gets the platform name for a player.
     * 
     * @param player the player
     * @return "Bedrock" or "Java"
     */
    public static String getPlatform(Player player) {
        return isBedrock(player) ? "§bBedrock" : "§aJava";
    }
}

