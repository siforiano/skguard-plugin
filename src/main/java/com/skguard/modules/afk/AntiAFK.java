package com.skguard.modules.afk;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;

public class AntiAFK implements SecurityModule, Listener {

    private final SKGuard plugin;
    private boolean enabled;
    private final Map<UUID, Long> lastActivity;
    private int afkTimeMinutes;
    private int warningTimeMinutes;
    private BukkitTask checkerTask;

    public AntiAFK(SKGuard plugin) {
        this.plugin = plugin;
        this.lastActivity = new ConcurrentHashMap<>();
    }

    @Override
    public String getName() {
        return "AntiAFK";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        reload();
        startAFKChecker();
    }

    @Override
    public void disable() {
        this.enabled = false;
        if (checkerTask != null) {
            checkerTask.cancel();
            checkerTask = null;
        }
        lastActivity.clear();
    }

    @Override
    public void reload() {
        this.afkTimeMinutes = plugin.getConfig().getInt("modules.AntiAFK.afk-time-minutes", 10);
        this.warningTimeMinutes = plugin.getConfig().getInt("modules.AntiAFK.warning-time-minutes", 8);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled)
            return;
        updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!enabled)
            return;
        org.bukkit.Location to = event.getTo();
        if (to != null && (event.getFrom().getX() != to.getX() ||
                event.getFrom().getZ() != to.getZ())) {
            updateActivity(event.getPlayer());
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!enabled)
            return;
        updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!enabled)
            return;
        updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!enabled)
            return;
        updateActivity(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastActivity.remove(event.getPlayer().getUniqueId());
    }

    private void updateActivity(Player player) {
        lastActivity.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private void startAFKChecker() {
        if (checkerTask != null)
            checkerTask.cancel();

        checkerTask = new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long afkThreshold = afkTimeMinutes * 60 * 1000L;
                long warningThreshold = warningTimeMinutes * 60 * 1000L;

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.hasPermission("SKGuard.bypass.afk"))
                        continue;

                    long lastActive = lastActivity.getOrDefault(player.getUniqueId(), now);
                    long idleTime = now - lastActive;

                    if (idleTime >= afkThreshold) {
                        player.kickPlayer(ChatColor.RED + "You were kicked for being AFK too long.");
                        plugin.getLogManager().logInfo("Kicked " + player.getName() + " for AFK");
                    } else if (idleTime >= warningThreshold) {
                        player.sendMessage(ChatColor.YELLOW + "Warning: You will be kicked for AFK in " +
                                (afkTimeMinutes - warningTimeMinutes) + " minutes!");
                    }
                }
            }
        }.runTaskTimer(plugin, 1200L, 1200L); // Every minute
    }
}

