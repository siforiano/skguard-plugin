package com.soulguard.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

import org.bukkit.entity.Player;

/**
 * Manages unique Hardware Identifiers (HWID) for players.
 * For Bedrock players, uses Geyser/Floodgate device data.
 * For Java players, uses a combination of available metadata.
 */
public class HWIDManager {

    /**
     * Generates a hardware fingerprint for a player.
     * 
     * @param player the player
     * @return a unique HWID string
     */
    public static String generateHWID(Player player) {
        StringBuilder sb = new StringBuilder();

        // 1. IP Address (Common factor)
        java.net.InetSocketAddress address = player.getAddress();
        if (address != null && address.getAddress() != null) {
            sb.append(address.getAddress().getHostAddress());
        }

        // 2. Client Brand (Java specific - often reveals client mod names)
        // Note: This requires the player to have sent the brand packet.
        try {
            String brand = (String) player.getClass().getMethod("getClientBrandName").invoke(player);
            if (brand != null)
                sb.append(brand);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | java.lang.reflect.InvocationTargetException ignored) {
        }

        // 3. Bedrock Specific Identifiers (Floodgate Extension)
        if (BedrockManager.isBedrock(player)) {
            try {
                org.geysermc.floodgate.api.FloodgateApi api = org.geysermc.floodgate.api.FloodgateApi.getInstance();
                org.geysermc.floodgate.api.player.FloodgatePlayer fPlayer = api.getPlayer(player.getUniqueId());

                if (fPlayer != null) {
                    sb.append(fPlayer.getDeviceOs().name());
                    sb.append(fPlayer.getLanguageCode());
                }
            } catch (NoClassDefFoundError | Exception ignored) {
                // Keep generic Exception here as floodgate API might throw various things
            }
        }

        // 4. Stable Metadata
        sb.append(player.getLocale());
        sb.append(player.getUniqueId().variant()); // Add variant for salt

        // 5. Handshake Entropy (If possible to access session data)
        // ... in future NMS versions we could add network buffer entropy

        return hash(sb.toString());
    }

    private static String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash).substring(0, 16);
        } catch (java.security.NoSuchAlgorithmException e) {
            return UUID.nameUUIDFromBytes(input.getBytes()).toString();
        }
    }
}
