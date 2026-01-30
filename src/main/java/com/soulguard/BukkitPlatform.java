package com.soulguard;

import com.soulguard.api.platform.SoulGuardPlatform;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class BukkitPlatform implements SoulGuardPlatform {

    private final SoulGuard plugin;

    public BukkitPlatform(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public void sendMessage(UUID uuid, String message) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.sendMessage(message);
        }
    }

    @Override
    public void kickPlayer(UUID uuid, String reason) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.kickPlayer(reason);
        }
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null && player.hasPermission(permission);
    }

    @Override
    public String getPlatformName() {
        return "Bukkit";
    }

    @Override
    public boolean isOnline(UUID uuid) {
        return Bukkit.getPlayer(uuid) != null;
    }

    @Override
    public void runAsync(Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    @Override
    public void runSync(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }
}
