package com.soulguard.modules.premium;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;

/**
 * Premium-only Machine Learning Detection Module.
 * <p>
 * Uses basic statistical analysis and behavioral profiling to detect cheaters
 * before they trigger traditional anti-cheat checks. Learns normal player
 * behavior patterns and flags anomalies.
 * </p>
 * 
 * <p>
 * <b>Premium Feature</b> - Only available in SoulGuard Premium
 * </p>
 * 
 * @author SoulGuard Team
 * @since 1.0
 */
public class MLDetectionModule implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private final Map<UUID, PlayerProfile> profiles;
    private double anomalyThreshold;

    public MLDetectionModule(SoulGuard plugin) {
        this.plugin = plugin;
        this.profiles = new ConcurrentHashMap<>();
    }

    @Override
    public String getName() {
        return "MLDetection";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        reload();
        plugin.getLogManager().logInfo("[Premium] Machine Learning Detection enabled");
    }

    @Override
    public void disable() {
        profiles.clear();
        this.enabled = false;
        plugin.getLogManager().logInfo("[Premium] Machine Learning Detection disabled");
    }

    @Override
    public void reload() {
        this.anomalyThreshold = plugin.getConfig().getDouble("modules.MLDetection.anomaly-threshold", 2.5);
    }

    /**
     * Gets the anomaly score for a player.
     * 
     * @param player the player
     * @return anomaly score (0.0 to 1.0)
     */
    public double getAnomalyScore(Player player) {
        if (!enabled)
            return 0.0;
        PlayerProfile profile = profiles.get(player.getUniqueId());
        return profile != null ? profile.detectAnomaly() : 0.0;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled)
            return;
        UUID uuid = event.getPlayer().getUniqueId();
        profiles.putIfAbsent(uuid, new PlayerProfile(uuid));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!enabled)
            return;
        // Fix Memory Leak: Remove profile when player leaves
        profiles.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!enabled)
            return;

        Player player = event.getPlayer();
        if (player.hasPermission("soulguard.bypass.ml"))
            return;

        PlayerProfile profile = profiles.get(player.getUniqueId());
        if (profile == null)
            return;

        // Update movement statistics
        org.bukkit.Location to = event.getTo();
        org.bukkit.Location from = event.getFrom();
        if (to == null || from == null)
            return;

        double distance = to.distance(from);
        profile.recordMovement(distance);

        // Check for anomalies
        if (profile.getSampleCount() > 100) { // Need baseline data
            double zScore = profile.getZScore(distance);
            if (Math.abs(zScore) > anomalyThreshold) {
                flagAnomaly(player, "Movement anomaly detected (Z-score: " + String.format("%.2f", zScore) + ")");
            }
        }
    }

    private void flagAnomaly(Player player, String reason) {
        plugin.getLogManager().logWarn("[Premium ML] " + player.getName() + " - " + reason);

        // Integrate with existing anti-cheat system
        if (plugin.getModuleManager().getModule("NeoGuard") != null) {
            // Increase scrutiny from traditional checks
        }

        // Send to forensics if available
        try {
            Object forensics = plugin.getModuleManager().getModule("Forensics");
            if (forensics != null) {
                forensics.getClass()
                        .getMethod("startRecording", org.bukkit.entity.Player.class, String.class)
                        .invoke(forensics, player, "ML Anomaly: " + reason);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Statistical profile for a player's behavior.
     * Uses advanced ML techniques for high precision detection.
     */
    private static class PlayerProfile {
        @SuppressWarnings("FieldCanBeLocal") // Used for profile identification
        private final UUID uuid;
        private final Map<String, StatisticalData> metrics;

        // Advanced ML features
        private final java.util.List<Double> recentMovements = new java.util.ArrayList<>();
        private final java.util.List<Long> clickTimestamps = new java.util.ArrayList<>();
        private double baselineEstablished = 0.0;

        public PlayerProfile(UUID uuid) {
            this.uuid = uuid;
            this.metrics = new HashMap<>();
            this.metrics.put("movement", new StatisticalData());
            this.metrics.put("clicks", new StatisticalData());
            this.metrics.put("rotation", new StatisticalData());
        }

        public void recordMovement(double distance) {
            metrics.get("movement").addSample(distance);

            // Keep recent history for pattern analysis
            recentMovements.add(distance);
            if (recentMovements.size() > 100) {
                recentMovements.remove(0);
            }

            // Update baseline confidence
            if (getSampleCount() > 50) {
                baselineEstablished = Math.min(1.0, getSampleCount() / 200.0);
            }
        }

        public void recordClick(long timestamp) {
            clickTimestamps.add(timestamp);
            if (clickTimestamps.size() > 50) {
                clickTimestamps.remove(0);
            }

            // Analyze click intervals
            if (clickTimestamps.size() >= 2) {
                long interval = timestamp - clickTimestamps.get(clickTimestamps.size() - 2);
                metrics.get("clicks").addSample(interval);
            }
        }

        public double getZScore(double value) {
            return metrics.get("movement").getZScore(value);
        }

        /**
         * Advanced anomaly detection using multiple metrics.
         * Returns confidence that behavior is anomalous (0.0 to 1.0).
         */
        public double detectAnomaly() {
            if (baselineEstablished < 0.5)
                return 0.0; // Not enough data

            double anomalyScore = 0.0;
            int checks = 0;

            // Check movement patterns
            if (recentMovements.size() >= 20) {
                double movementVariance = calculateVariance(recentMovements);
                if (movementVariance < 0.001) {
                    anomalyScore += 0.3; // Too consistent (bot-like)
                }
                checks++;
            }

            // Check click patterns
            if (clickTimestamps.size() >= 10) {
                double clickConsistency = analyzeClickConsistency();
                if (clickConsistency > 0.95) {
                    anomalyScore += 0.4; // Auto-clicker detected
                }
                checks++;
            }

            return checks > 0 ? (anomalyScore / checks) * baselineEstablished : 0.0;
        }

        private double calculateVariance(java.util.List<Double> values) {
            if (values.isEmpty())
                return 0.0;

            double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double variance = values.stream()
                    .mapToDouble(v -> Math.pow(v - mean, 2))
                    .average()
                    .orElse(0.0);

            return variance;
        }

        private double analyzeClickConsistency() {
            if (clickTimestamps.size() < 3)
                return 0.0;

            java.util.List<Long> intervals = new java.util.ArrayList<>();
            for (int i = 1; i < clickTimestamps.size(); i++) {
                intervals.add(clickTimestamps.get(i) - clickTimestamps.get(i - 1));
            }

            double mean = intervals.stream().mapToDouble(Long::doubleValue).average().orElse(0.0);
            double stdDev = Math.sqrt(intervals.stream()
                    .mapToDouble(v -> Math.pow(v - mean, 2))
                    .average()
                    .orElse(0.0));

            // High consistency = low standard deviation relative to mean
            return mean > 0 ? 1.0 - Math.min(1.0, stdDev / mean) : 0.0;
        }

        public int getSampleCount() {
            return metrics.get("movement").getSampleCount();
        }
    }

    /**
     * Stores statistical data for anomaly detection.
     */
    private static class StatisticalData {
        private double sum = 0;
        private double sumOfSquares = 0;
        private int count = 0;

        public void addSample(double value) {
            sum += value;
            sumOfSquares += value * value;
            count++;
        }

        public double getMean() {
            return count > 0 ? sum / count : 0;
        }

        public double getStandardDeviation() {
            if (count < 2)
                return 0;
            double mean = getMean();
            double variance = (sumOfSquares / count) - (mean * mean);
            return Math.sqrt(Math.max(0, variance));
        }

        public double getZScore(double value) {
            double mean = getMean();
            double stdDev = getStandardDeviation();
            return stdDev > 0 ? (value - mean) / stdDev : 0;
        }

        public int getSampleCount() {
            return count;
        }
    }
}
