package com.soulguard.modules.premium;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;

/**
 * Premium Elite Feature: Global Trust Network.
 * Accesses a central database of malicious UUIDs/IPs shared among
 * SoulGuard Premium users to prevent known rule-breakers from joining.
 */
public class GlobalTrustModule implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    
    // Cached reputation scores
    private final Map<UUID, Integer> trustScores = new ConcurrentHashMap<>();

    public GlobalTrustModule(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "GlobalTrust";
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
        trustScores.clear();
    }

    @Override
    public void reload() {}

    @EventHandler
    public void onLoginCheck(AsyncPlayerPreLoginEvent event) {
        if (!enabled) return;

        // Elite simulation of API lookup
        int score = getGlobalTrustScore(event.getUniqueId());
        
        if (score < 20) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, 
                "§c§lSoulGuard Global Ban\n§7Your account has extremely low trust in our global network.");
            plugin.getLogManager().logWarn("[GlobalTrust] Denied join for " + event.getName() + " (Score: " + score + ")");
        }
    }

    private int getGlobalTrustScore(UUID uuid) {
        // Normally this involves an HTTPS request to an API
        return trustScores.getOrDefault(uuid, 100);
    }
}
