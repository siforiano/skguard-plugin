package com.soulguard.modules.panic;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import com.soulguard.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class PanicModule implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private boolean panicActive;
    private String panicMessage;

    public PanicModule(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "PanicMode";
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
        this.panicActive = false;
    }

    @Override
    public void reload() {
        this.panicMessage = plugin.getConfig().getString("modules.PanicMode.message",
                "&c&lEMERGENCY LOCKDOWN\n&7The server is currently in panic mode.");
    }

    public boolean isPanicActive() {
        return panicActive;
    }

    public void setPanicActive(boolean active) {
        if (!enabled)
            return;
        this.panicActive = active;
        if (active) {
            plugin.getLogManager().logWarn("PANIC MODE ACTIVATED!");
            kickNonStaff();
        } else {
            plugin.getLogManager().logInfo("Panic mode deactivated.");
        }
    }

    private void kickNonStaff() {
        String msg = ColorUtil.translate(panicMessage);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("soulguard.staff") && !player.isOp()) {
                player.kickPlayer(msg);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        if (enabled && panicActive) {
            // We can't check permissions easily here, so we block everyone
            // except those explicitly whitelisted by UUID or name if we had a list.
            // For now, panic blocks all new connections.
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ColorUtil.translate(panicMessage));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (enabled && panicActive && !event.getPlayer().hasPermission("soulguard.admin")) {
            event.setCancelled(true);
            event.getPlayer()
                    .sendMessage(ColorUtil.translate("&c[SoulGuard] Commands are disabled during Panic Mode."));
        }
    }
}
