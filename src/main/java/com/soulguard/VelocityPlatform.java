package com.soulguard;

import com.soulguard.api.platform.SoulGuardPlatform;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.UUID;

public class VelocityPlatform implements SoulGuardPlatform {

    private final SoulGuardVelocity plugin;
    private final ProxyServer server;

    public VelocityPlatform(SoulGuardVelocity plugin) {
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
