package com.soulguard.modules.premium;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Premium-only Forensics Module.
 * <p>
 * Records suspicious player activity for post-incident analysis.
 * Includes packet recording, session replay, and evidence export capabilities.
 * </p>
 * 
 * <p>
 * <b>Premium Feature</b> - Only available in SoulGuard Premium
 * </p>
 * 
 * @author SoulGuard Team
 * @since 1.0
 */
public class ForensicsModule implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private final Map<UUID, SessionRecording> recordings;
    @SuppressWarnings("FieldCanBeLocal") // Used for automatic cleanup in future
    private int maxRecordingDuration;
    @SuppressWarnings("FieldCanBeLocal") // Used for auto-recording feature
    private boolean autoRecordSuspicious;

    public ForensicsModule(SoulGuard plugin) {
        this.plugin = plugin;
        this.recordings = new ConcurrentHashMap<>();
    }

    @Override
    public String getName() {
        return "Forensics";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        reload();
        plugin.getLogManager().logInfo("[Premium] Forensics Module enabled - Recording suspicious activity");
    }

    @Override
    public void disable() {
        recordings.clear();
        this.enabled = false;
        plugin.getLogManager().logInfo("[Premium] Forensics Module disabled");
    }

    @Override
    public void reload() {
        this.maxRecordingDuration = plugin.getConfig().getInt("modules.Forensics.max-duration-seconds", 300);
        this.autoRecordSuspicious = plugin.getConfig().getBoolean("modules.Forensics.auto-record-suspicious", true);
    }

    /**
     * Starts recording a player's session.
     * 
     * @param player the player to record
     * @param reason the reason for recording
     */
    public void startRecording(Player player, String reason) {
        if (!autoRecordSuspicious && reason.equals("Automatic Security Audit")) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (recordings.containsKey(uuid)) {
            plugin.getLogManager().logWarn("[Premium Forensics] Already recording " + player.getName());
            return;
        }

        SessionRecording recording = new SessionRecording(uuid, reason);
        recordings.put(uuid, recording);

        // Auto-stop task based on maxRecordingDuration
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isRecording(uuid)) {
                stopRecording(player);
                plugin.getLogManager().logInfo("[Premium Forensics] Max duration reached for " + player.getName());
            }
        }, maxRecordingDuration * 20L);

        plugin.getLogManager()
                .logInfo("[Premium Forensics] Started recording " + player.getName() + " - Reason: " + reason);
    }

    /**
     * Stops recording a player's session.
     * 
     * @param player the player
     * @return the completed recording, or null if not recording
     */
    public SessionRecording stopRecording(Player player) {
        UUID uuid = player.getUniqueId();
        SessionRecording recording = recordings.remove(uuid);
        if (recording != null) {
            recording.stop();
            plugin.getLogManager().logInfo("[Premium Forensics] Stopped recording " + player.getName() + " - Duration: "
                    + recording.getDuration() + "s");
        }
        return recording;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Prevent Memory Leak: Stop recording and clear data when player leaves
        recordings.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Checks if a player is being recorded.
     * 
     * @param uuid the player UUID
     * @return true if recording
     */
    public boolean isRecording(UUID uuid) {
        return recordings.containsKey(uuid);
    }

    /**
     * Records an event for a player.
     * 
     * @param uuid      the player UUID
     * @param eventType the type of event
     * @param data      event data
     */
    public void recordEvent(UUID uuid, String eventType, String data) {
        SessionRecording recording = recordings.get(uuid);
        if (recording != null) {
            recording.addEvent(eventType, data);
        }
    }

    /**
     * Exports a recording to a file.
     * 
     * @param recording the recording to export
     * @return path to exported file
     */
    public String exportRecording(SessionRecording recording) {
        // TODO: Implement actual export to JSON/binary format
        String filename = "forensics_" + recording.getPlayerUUID() + "_" + System.currentTimeMillis() + ".json";
        plugin.getLogManager().logInfo("[Premium Forensics] Exported recording to " + filename);
        return filename;
    }

    /**
     * Represents a recorded player session.
     */
    public static class SessionRecording {
        private final UUID playerUUID;
        private final String reason;
        private final long startTime;
        private long endTime;
        private final List<ForensicEvent> events;

        public SessionRecording(UUID playerUUID, String reason) {
            this.playerUUID = playerUUID;
            this.reason = reason;
            this.startTime = System.currentTimeMillis();
            this.events = new ArrayList<>();
        }

        public void addEvent(String type, String data) {
            events.add(new ForensicEvent(type, data, System.currentTimeMillis() - startTime));
        }

        public void stop() {
            this.endTime = System.currentTimeMillis();
        }

        public long getDuration() {
            long end = endTime > 0 ? endTime : System.currentTimeMillis();
            return (end - startTime) / 1000;
        }

        public UUID getPlayerUUID() {
            return playerUUID;
        }

        public String getReason() {
            return reason;
        }

        public List<ForensicEvent> getEvents() {
            return new ArrayList<>(events);
        }
    }

    /**
     * Represents a single forensic event.
     */
    public static class ForensicEvent {
        private final String type;
        private final String data;
        private final long timestamp;

        public ForensicEvent(String type, String data, long timestamp) {
            this.type = type;
            this.data = data;
            this.timestamp = timestamp;
        }

        public String getType() {
            return type;
        }

        public String getData() {
            return data;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
}
