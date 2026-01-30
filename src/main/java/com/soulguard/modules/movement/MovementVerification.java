package com.soulguard.modules.movement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import com.soulguard.util.ColorUtil;

public class MovementVerification implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private final Set<UUID> pendingVerification;
    private final Map<UUID, Location> joinLocations;
    private int verificationTime; // seconds

    public MovementVerification(SoulGuard plugin) {
        this.plugin = plugin;
        this.pendingVerification = new HashSet<>();
        this.joinLocations = new HashMap<>();
    }

    @Override
    public String getName() {
        return "MovementVerification";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        reload();
    }

    @Override
    public void disable() {
        this.enabled = false;
        pendingVerification.clear();
        joinLocations.clear();
    }

    @Override
    public void reload() {
        this.verificationTime = plugin.getConfig().getInt("modules.MovementVerification.verification-time", 10);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled)
            return;

        Player player = event.getPlayer();
        if (player.hasPermission("soulguard.bypass.movement"))
            return;

        pendingVerification.add(player.getUniqueId());
        joinLocations.put(player.getUniqueId(), player.getLocation().clone());

        player.sendMessage(
                ChatColor.YELLOW + "[SoulGuard] Por favor, muévete un poco para verificar que eres humano.");

        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingVerification.contains(player.getUniqueId())) {
                    player.kickPlayer(ColorUtil.translate("&a[SoulGuard] Verificación de usuario completada.\n&7Por favor, conéctate de nuevo."));
                    // Log as standard timeout, not bot detection
                    plugin.getLogManager().logInfo("Player " + player.getName() + " timed out during movement verification.");
                }
            }
        }.runTaskLater(plugin, verificationTime * 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        if (!enabled)
            return;

        UUID uuid = event.getPlayer().getUniqueId();
        if (!pendingVerification.contains(uuid))
            return;

        Location from = event.getFrom();
        Location to = event.getTo();

        // Check if player actually moved (not just head rotation)
        if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
            pendingVerification.remove(uuid);
            joinLocations.remove(uuid);
            event.getPlayer().sendMessage(ChatColor.GREEN + "¡Verificación de movimiento correcta!");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingVerification.remove(event.getPlayer().getUniqueId());
        joinLocations.remove(event.getPlayer().getUniqueId());
    }
}
