package com.skguard.modules.geo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;

public class TravelGuard implements SecurityModule, Listener {

    private final SKGuard plugin;
    private boolean enabled;
    private final Map<UUID, LoginRecord> lastLogins = new HashMap<>();

    public TravelGuard(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "TravelGuard";
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
        lastLogins.clear();
    }

    @Override
    public void reload() {
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!enabled)
            return;

        UUID uuid = event.getUniqueId();
        String name = event.getName();
        String currentIp = event.getAddress().getHostAddress();
        long now = System.currentTimeMillis();

        if (lastLogins.containsKey(uuid)) {
            LoginRecord last = lastLogins.get(uuid);

            // Impossible Travel: If IP changes and time is very short
            if (!last.ip.equals(currentIp)) {
                long timeDiffSeconds = (now - last.timestamp) / 1000;

                // Impossible Travel: If IP changed in less than 30 minutes
                if (timeDiffSeconds < 1800) {
                    plugin.getLogManager()
                            .logWarn("TRAVEL ALERT for " + name + ": Impossible jump in " + timeDiffSeconds + "s.");

                    if (plugin.getDiscordWebhook().isEnabled()) {
                        plugin.getDiscordWebhook().sendAlert("Security Alert: Impossible Travel",
                                "Player **" + name + "** changed IP significantly in " + timeDiffSeconds + "s.\n" +
                                        "**Last IP:** " + last.ip + "\n**New IP:** " + currentIp,
                                16753920);
                    }

                    if (plugin.getUserSecurity() != null && plugin.getUserSecurity().isEnabled()) {
                        plugin.getUserSecurity().setLocked(uuid, true);
                        plugin.getLogManager().logWarn("Account " + name + " auto-locked due to impossible travel.");
                    }
                }
            }
        }

        lastLogins.put(uuid, new LoginRecord(currentIp, now));
    }

    private static class LoginRecord {
        final String ip;
        final long timestamp;

        LoginRecord(String ip, long timestamp) {
            this.ip = ip;
            this.timestamp = timestamp;
        }
    }
}

