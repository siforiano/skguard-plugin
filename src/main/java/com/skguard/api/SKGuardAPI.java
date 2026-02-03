package com.skguard.api;

import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.entity.Player;

import com.skguard.SKGuard;
import com.skguard.modules.anticheat.Check;

/**
 * Public API for SKGuard Premium.
 * <p>
 * Allows third-party developers to integrate with SKGuard,
 * register custom checks, listen to events, and access player data.
 * </p>
 * 
 * <p><b>Example Usage:</b></p>
 * <pre>{@code
 * // Get API instance
 * SKGuardAPI api = SKGuardAPI.getInstance();
 * 
 * // Register custom check
 * api.registerCheck(new MyCustomCheck(plugin));
 * 
 * // Listen to cheat detection
 * api.onCheatDetected(event -> {
 *     Player player = event.getPlayer();
 *     String checkName = event.getCheckName();
 *     // Your custom logic
 * });
 * 
 * // Get trust score
 * double trust = api.getTrustScore(player);
 * }</pre>
 * 
 * <p><b>Premium Feature</b> - Only available in SKGuard Premium</p>
 * 
 * @author SKGuard Team
 * @since 1.0
 */
public class SKGuardAPI {

    private static SKGuardAPI instance;
    private final SKGuard plugin;
    private final java.util.List<Consumer<CheatDetectionEvent>> cheatListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.List<Consumer<PlayerTrustEvent>> trustListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    private SKGuardAPI(SKGuard plugin) {
        this.plugin = plugin;
    }

    /**
     * Gets the SKGuardAPI instance.
     * 
     * @return the API instance, or null if not available
     */
    public static SKGuardAPI getInstance() {
        if (instance == null && SKGuard.getInstance() != null) {
            instance = new SKGuardAPI(SKGuard.getInstance());
        }
        return instance;
    }

    /**
     * Fires a cheat detection event to all registered listeners.
     */
    public void fireCheatDetected(Player player, String checkName, String details, double confidence) {
        CheatDetectionEvent event = new CheatDetectionEvent(player, checkName, details, confidence);
        cheatListeners.forEach(listener -> {
            try {
                listener.accept(event);
            } catch (Exception e) {
                plugin.getLogManager().logError("[API] Error in cheat detection listener: " + e.getMessage());
            }
        });
    }

    /**
     * Fires a player trust event to all registered listeners.
     */
    public void firePlayerTrustChanged(Player player, double oldTrust, double newTrust) {
        PlayerTrustEvent event = new PlayerTrustEvent(player, oldTrust, newTrust);
        trustListeners.forEach(listener -> {
            try {
                listener.accept(event);
            } catch (Exception e) {
                plugin.getLogManager().logError("[API] Error in player trust listener: " + e.getMessage());
            }
        });
    }

    /**
     * Registers a custom anti-cheat check.
     * <p>
     * The check will be automatically enabled/disabled based on config.
     * </p>
     * 
     * @param check the custom check to register
     * @throws IllegalStateException if not running Premium edition
     */
    public void registerCheck(Check check) {
        if (!plugin.getEdition().isPremium()) {
            throw new IllegalStateException("Custom checks require SKGuard Premium");
        }
        
        com.skguard.modules.anticheat.CheckManager manager = 
            plugin.getModuleManager().getModule(com.skguard.modules.anticheat.CheckManager.class);
        
        if (manager != null) {
            manager.getChecks().add(check);
            plugin.getLogManager().logInfo("[API] Registered custom check: " + check.getName());
        }
    }

    /**
     * Registers a listener for cheat detection events.
     * 
     * @param listener the event listener
     */
    public void onCheatDetected(Consumer<CheatDetectionEvent> listener) {
        cheatListeners.add(listener);
    }

    /**
     * Registers a listener for player trust events.
     * 
     * @param listener the event listener
     */
    public void onPlayerTrusted(Consumer<PlayerTrustEvent> listener) {
        trustListeners.add(listener);
    }

    /**
     * Gets the trust score for a player.
     * <p>
     * Trust score ranges from 0.0 (untrusted) to 1.0 (fully trusted).
     * </p>
     * 
     * @param player the player
     * @return trust score, or 1.0 if biometry module is disabled
     */
    public double getTrustScore(Player player) {
        try {
            com.skguard.api.SecurityModule module = plugin.getModuleManager().getModule("Biometry");
            if (module == null) return 1.0;
            
            Object biometry = Class.forName("com.skguard.modules.premium.BiometryModule")
                .getMethod("verifyIdentity", Player.class)
                .invoke(module, player);
            return (double) biometry;
        } catch (Exception e) {
            return 1.0; // Default trust
        }
    }

    /**
     * Gets the trust score for a player by UUID.
     * 
     * @param uuid the player UUID
     * @return trust score, or 1.0 if player not found
     */
    public double getTrustScore(UUID uuid) {
        Player player = plugin.getServer().getPlayer(uuid);
        return player != null ? getTrustScore(player) : 1.0;
    }

    /**
     * Gets the behavior profile for a player.
     * 
     * @param player the player
     * @return behavior profile, or null if ML module is disabled
     */
    public BehaviorProfile getBehaviorProfile(Player player) {
        try {
            com.skguard.api.SecurityModule mlModule = plugin.getModuleManager().getModule("MLDetection");
            if (mlModule != null) {
                // TODO: Return actual profile
                return new BehaviorProfile(player.getUniqueId());
            }
        } catch (Exception e) {
            // ML module not available
        }
        return null;
    }

    /**
     * Checks if a player is currently being recorded by forensics.
     * 
     * @param uuid the player UUID
     * @return true if recording
     */
    public boolean isRecording(UUID uuid) {
        try {
            com.skguard.api.SecurityModule forensics = plugin.getModuleManager().getModule("Forensics");
            if (forensics != null) {
                return (boolean) forensics.getClass()
                    .getMethod("isRecording", UUID.class)
                    .invoke(forensics, uuid);
            }
        } catch (Exception e) {
            // Forensics module not available
        }
        return false;
    }

    /**
     * Starts recording a player's session.
     * 
     * @param player the player to record
     * @param reason the reason for recording
     */
    public void startRecording(Player player, String reason) {
        try {
            com.skguard.api.SecurityModule forensics = plugin.getModuleManager().getModule("Forensics");
            if (forensics != null) {
                forensics.getClass()
                    .getMethod("startRecording", Player.class, String.class)
                    .invoke(forensics, player, reason);
            }
        } catch (Exception e) {
            plugin.getLogManager().logError("[API] Failed to start recording: " + e.getMessage());
        }
    }

    /**
     * Event fired when a cheat is detected.
     */
    public static class CheatDetectionEvent {
        private final Player player;
        private final String checkName;
        private final String details;
        private final double confidence;

        public CheatDetectionEvent(Player player, String checkName, String details, double confidence) {
            this.player = player;
            this.checkName = checkName;
            this.details = details;
            this.confidence = confidence;
        }

        public Player getPlayer() { return player; }
        public String getCheckName() { return checkName; }
        public String getDetails() { return details; }
        public double getConfidence() { return confidence; }
    }

    /**
     * Event fired when a player's trust level changes.
     */
    public static class PlayerTrustEvent {
        private final Player player;
        private final double oldTrust;
        private final double newTrust;

        public PlayerTrustEvent(Player player, double oldTrust, double newTrust) {
            this.player = player;
            this.oldTrust = oldTrust;
            this.newTrust = newTrust;
        }

        public Player getPlayer() { return player; }
        public double getOldTrust() { return oldTrust; }
        public double getNewTrust() { return newTrust; }
    }

    /**
     * Represents a player's behavior profile.
     */
    public static class BehaviorProfile {
        private final UUID uuid;

        public BehaviorProfile(UUID uuid) {
            this.uuid = uuid;
        }

        public UUID getUUID() {
            return uuid;
        }

        // TODO: Add more profile data methods
    }
}

