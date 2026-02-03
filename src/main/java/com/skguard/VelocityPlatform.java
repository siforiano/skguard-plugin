package com.skguard;

import java.util.UUID;

import com.skguard.api.platform.SKGuardPlatform;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class VelocityPlatform implements SKGuardPlatform {

    private final SKGuardVelocity plugin;
    private final ProxyServer server;

    public VelocityPlatform(SKGuardVelocity plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
    }

    @Override
    public void sendMessage(UUID uuid, String message) {
        server.getPlayer(uuid).ifPresent(player -> 
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message))
        );
    }

    @Override
    public void kickPlayer(UUID uuid, String reason) {
        server.getPlayer(uuid).ifPresent(player -> 
            player.disconnect(LegacyComponentSerializer.legacyAmpersand().deserialize(reason))
        );
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        return server.getPlayer(uuid).map(player -> player.hasPermission(permission)).orElse(false);
    }

    @Override
    public String getPlatformName() {
        return "Velocity";
    }

    @Override
    public boolean isOnline(UUID uuid) {
        return server.getPlayer(uuid).isPresent();
    }

    @Override
    public void runAsync(Runnable runnable) {
        server.getScheduler().buildTask(plugin, runnable).schedule();
    }

    @Override
    public void runSync(Runnable runnable) {
        // Velocity is mostly async, "sync" tasks aren't really a thing like in Bukkit
        runAsync(runnable);
    }
}

