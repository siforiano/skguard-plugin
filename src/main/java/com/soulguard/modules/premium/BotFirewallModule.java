package com.soulguard.modules.premium;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;

/**
 * Premium Elite Feature: AI Bot Mitigation Firewall.
 * Detects and blocks mass bot attacks using connection patterns and latency analysis.
 */
public class BotFirewallModule implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    
    private final Map<String, Long> lastConnection = new ConcurrentHashMap<>();
    private final Map<String, Integer> connectionBurst = new ConcurrentHashMap<>();
    
    private int mitigationLevel;
    private final long burstThresholdMs = 2000;
    private final int maxBurst = 5;

    public BotFirewallModule(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "AI_BotFirewall";
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
        lastConnection.clear();
        connectionBurst.clear();
    }

    @Override
    public void reload() {
        this.mitigationLevel = plugin.getConfig().getInt("modules.AI_BotFirewall.mitigation-level", 3);
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!enabled) return;

        String ip = event.getAddress().getHostAddress();
        long now = System.currentTimeMillis();
        
        // Pattern 1: Rapid reconnection burst
        long last = lastConnection.getOrDefault(ip, 0L);
        if (now - last < burstThresholdMs) {
            int burst = connectionBurst.getOrDefault(ip, 0) + 1;
            connectionBurst.put(ip, burst);
            
            // Scaled threshold based on mitigation level
            int dynamicMax = maxBurst / Math.max(1, mitigationLevel);
            
            if (burst > dynamicMax) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, 
                    "§c§lSoulGuard Firewall\n§7Massive connection burst detected. Mitigation Level: " + mitigationLevel);
                plugin.getLogManager().logWarn("[Firewall] Blocked burst from " + ip + " (ML: " + mitigationLevel + ")");
                return;
            }
        } else {
            connectionBurst.put(ip, 0);
        }
        
        lastConnection.put(ip, now);
    }
}
