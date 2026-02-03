package com.skguard.modules.punish;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;
import com.skguard.database.PunishmentManager.Punishment;
import com.skguard.database.PunishmentManager.PunishmentType;
import com.skguard.util.ColorUtil;
import com.skguard.util.TimeUtil;

public class ModerationModule implements SecurityModule, Listener {

    private final SKGuard plugin;
    private boolean enabled;

    public ModerationModule(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Moderation";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
    }

    @Override
    public void disable() {
        this.enabled = false;
    }

    @Override
    public void reload() {
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        if (!enabled) return;

        UUID uuid = event.getUniqueId();
        
        // CACHE LOAD (Blocking but on Async Thread -> OK)
        plugin.getPunishmentManager().loadCache(uuid);
        
        Punishment ban = plugin.getPunishmentManager().getActivePunishment(uuid, PunishmentType.BAN);

        if (ban != null) {
            String timeStr = ban.expiry > 0 ? " (" + TimeUtil.formatTimeLeft(ban.expiry) + ")" : "";
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    ColorUtil.translate("&c&l[SKGuard] &7You are BANNED" + timeStr + "\n&fReason: &7" + ban.reason));
        }
    }

    @EventHandler
    public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        plugin.getPunishmentManager().unloadCache(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!enabled) return;

        Player player = event.getPlayer();
        Punishment mute = plugin.getPunishmentManager().getActivePunishment(player.getUniqueId(), PunishmentType.MUTE);

        if (mute != null) {
            event.setCancelled(true);
            String timeStr = mute.expiry > 0 ? " (" + TimeUtil.formatTimeLeft(mute.expiry) + ")" : "";
            player.sendMessage(ColorUtil.translate("&c&l[SKGuard] &7You are currently MUTED" + timeStr + "."));
            player.sendMessage(ColorUtil.translate("&fReason: &7" + mute.reason));
        }
    }
}

