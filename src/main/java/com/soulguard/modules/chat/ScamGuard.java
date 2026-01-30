package com.soulguard.modules.chat;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import com.soulguard.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Arrays;
import java.util.List;

public class ScamGuard implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private final List<String> scamKeywords = Arrays.asList(
            "contraseña", "password", "clave", "staff", "admin", "dueño", "owner", "mod",
            "regalando", "tienda gratis", "free rank", "pásame", "dame");

    public ScamGuard(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "ScamGuard";
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!enabled)
            return;

        String message = event.getMessage().toLowerCase();

        // Check for multiple keywords to reduce false positives
        long matchCount = scamKeywords.stream().filter(message::contains).count();

        if (matchCount >= 2) {
            alertStaff(event.getPlayer(), event.getMessage());
        }
    }

    private void alertStaff(Player player, String message) {
        String alert = ColorUtil.translate("&c&l[ScamGuard] &7Potential social engineering from &e" + player.getName()
                + "&7: &f\"" + message + "\"");

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("soulguard.admin")) {
                p.sendMessage(alert);
            }
        }

        plugin.getLogManager().logWarn("Scam alert for " + player.getName() + ": " + message);

        if (plugin.getDiscordWebhook().isEnabled()) {
            plugin.getDiscordWebhook().sendAlert("Social Engineering Alert",
                    "**Player:** " + player.getName() + "\n**Message:** " + message, 16753920);
        }
    }
}
