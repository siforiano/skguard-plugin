package com.soulguard.modules.anticheat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;

public class CheckManager implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private final List<Check> checks = new ArrayList<>();
    private double lastTPS = 20.0;

    public CheckManager(SoulGuard plugin) {
        this.plugin = plugin;
        startTpsTracker();
    }

    private void startTpsTracker() {
        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            long lastRun = System.currentTimeMillis();
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long diff = now - lastRun;
                lastRun = now;
                lastTPS = Math.min(20.0, 20.0 / (diff / 1000.0));
            }
        }, 20L, 20L);
    }

    public double getLastTPS() {
        return lastTPS;
    }

    public SoulGuard getPlugin() {
        return plugin;
    }

    @Override
    public String getName() {
        return "NeoGuard";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        // Registration of checks will happen here
    }

    @Override
    public void disable() {
        this.enabled = false;
        playerDataMap.clear();
        checks.clear();
    }

    private final List<Check> enabledChecks = new ArrayList<>();

    @Override
    public void reload() {
        checks.forEach(Check::reload);
        updateEnabledChecks();
    }

    private void updateEnabledChecks() {
        enabledChecks.clear();
        for (Check check : checks) {
            if (check.isEnabled()) {
                enabledChecks.add(check);
            }
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, PlayerData::new);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        playerDataMap.put(event.getPlayer().getUniqueId(), new PlayerData(event.getPlayer().getUniqueId()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerDataMap.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!enabled)
            return;

        // OPTIMIZATION: Check if checks are registered (Init phase)
        if (enabledChecks.isEmpty() && !checks.isEmpty()) {
            updateEnabledChecks();
        }

        PlayerData data = getPlayerData(event.getPlayer().getUniqueId());
        if (data == null)
            return;

        // Update data tracking
        org.bukkit.Location to = event.getTo();
        if (to == null)
            return;

        data.setLastLocation(event.getFrom());
        data.setLastDeltaY(to.getY() - event.getFrom().getY());
        @SuppressWarnings("deprecation")
        boolean onGround = event.getPlayer().isOnGround();
        data.setLastOnGround(onGround);
        data.setLastMoveTime(System.currentTimeMillis());

        // Centralized Dispatch: Using pre-filtered enabled list (Performance++)
        for (Check check : enabledChecks) {
            check.handleMove(event, data);
        }
    }

    public List<Check> getChecks() {
        return checks;
    }
}
