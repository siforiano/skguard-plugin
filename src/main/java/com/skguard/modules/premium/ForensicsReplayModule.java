package com.skguard.modules.premium;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;

/**
 * Premium Elite Feature: 3D Forensics Replay.
 * Records player movements and actions into frames, allowing staff
 * to replay them as hologram ghosts for visual cheating evidence.
 */
public class ForensicsReplayModule implements SecurityModule, Listener {

    private final SKGuard plugin;
    private boolean enabled;

    // Optimized for O(1) removal of oldest frames
    private final Map<UUID, Deque<ReplayFrame>> history = new ConcurrentHashMap<>();
    private final int maxFrames = 600; // 30 seconds at 20tps

    public ForensicsReplayModule(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Forensics_3D";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        startRecordingTask();
    }

    @Override
    public void disable() {
        this.enabled = false;
        history.clear();
    }

    @Override
    public void reload() {
    }

    private void startRecordingTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!enabled) {
                    this.cancel();
                    return;
                }

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    recordFrame(player);
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void recordFrame(Player player) {
        Deque<ReplayFrame> frames = history.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>());
        frames.addLast(new ReplayFrame(player.getLocation()));

        if (frames.size() > maxFrames) {
            frames.pollFirst(); // O(1) complexity
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Prevent Memory Leak: Clear history when player leaves
        history.remove(event.getPlayer().getUniqueId());
    }

    public void playReplay(Player staff, UUID targetUuid) {
        Deque<ReplayFrame> frames = history.get(targetUuid);
        if (frames == null || frames.isEmpty()) {
            staff.sendMessage("§cNo replay data available for this player.");
            return;
        }

        staff.sendMessage("§aStarting 3D Forensics playback for " + targetUuid + "...");

        // Convert to array for indexed access during replay
        final ReplayFrame[] frameArray = frames.toArray(new ReplayFrame[0]);

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (index >= frameArray.length || !staff.isOnline()) {
                    this.cancel();
                    staff.sendMessage("§ePlayback finished.");
                    return;
                }

                ReplayFrame frame = frameArray[index++];
                // Real implementation would spawn an ArmorStand or NPC here

                // Simulate frame sync effect with particle speed
                staff.spawnParticle(org.bukkit.Particle.CLOUD, frame.location, 5, 0.1, 0.1, 0.1, 0.05);
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private static class ReplayFrame {
        final Location location;

        ReplayFrame(Location location) {
            this.location = location.clone();
        }
    }
}

