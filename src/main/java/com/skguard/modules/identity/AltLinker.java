package com.skguard.modules.identity;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;

public class AltLinker implements SecurityModule, Listener {

    private final SKGuard plugin;
    private boolean enabled;
    private final Map<String, Set<UUID>> ipToAccounts = new ConcurrentHashMap<>();
    private final Map<UUID, Fingerprint> fingerprints = new ConcurrentHashMap<>();

    public AltLinker(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "AltLinker";
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
        ipToAccounts.clear();
        fingerprints.clear();
    }

    @Override
    public void reload() {
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        if (!enabled)
            return;
        String ip = event.getAddress().getHostAddress();
        ipToAccounts.computeIfAbsent(ip, k -> ConcurrentHashMap.newKeySet()).add(event.getUniqueId());
    }

    public java.util.Set<UUID> getAlts(UUID target) {
        for (Set<UUID> uuids : ipToAccounts.values()) {
            if (uuids.contains(target)) {
                return uuids;
            }
        }
        return new java.util.HashSet<>(java.util.Collections.singletonList(target));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled)
            return;
        Player player = event.getPlayer();

        java.net.InetSocketAddress addr = player.getAddress();
        if (addr == null || addr.getAddress() == null) return;
        Fingerprint current = new Fingerprint(
                addr.getAddress().getHostAddress(),
                player.getLocale(),
                Bukkit.getBukkitVersion());

        fingerprints.put(player.getUniqueId(), current);

        // Detailed check for alt accounts
        checkAlts(player, current);
    }

    private void checkAlts(Player player, Fingerprint current) {
        Set<UUID> alts = ipToAccounts.get(current.ip);
        if (alts != null && alts.size() > 1) {
            plugin.getLogManager().logInfo("[AltLinker] Possible alts for " + player.getName() + ": " + alts.size());

            // If they are under staff investigation or have previous bans, we could
            // auto-quarantine
            if (alts.size() > 5) {
                plugin.getLogManager().logWarn("Excessive alt accounts detected for IP " + current.ip);
            }
        }
    }

    private static class Fingerprint {
        final String ip;
        final String locale;
        final String version;

        Fingerprint(String ip, String locale, String version) {
            this.ip = ip;
            this.locale = locale;
            this.version = version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Fingerprint that = (Fingerprint) o;
            return Objects.equals(ip, that.ip) && Objects.equals(locale, that.locale)
                    && Objects.equals(version, that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ip, locale, version);
        }
    }
}

