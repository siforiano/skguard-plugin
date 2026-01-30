package com.soulguard.modules.vpn;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;

public class VPNDetector implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private final Map<String, Boolean> vpnCache;
    private String proxyCheckKey;
    private String ipHubKey;
    private boolean blockHosting;
    private boolean blockVPN;
    private boolean blockProxy;
    private int riskThreshold;
    private java.util.List<String> blockedASNs;

    public VPNDetector(SoulGuard plugin) {
        this.plugin = plugin;
        this.vpnCache = new ConcurrentHashMap<>();
    }

    @Override
    public String getName() {
        return "VPNDetector";
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
        this.vpnCache.clear();
    }

    @Override
    public void reload() {
        this.proxyCheckKey = plugin.getConfig().getString("modules.VPNDetector.proxycheck-key", "");
        this.ipHubKey = plugin.getConfig().getString("modules.VPNDetector.iphub-key", "");
        this.blockHosting = plugin.getConfig().getBoolean("modules.VPNDetector.block-hosting", true);
        this.blockVPN = plugin.getConfig().getBoolean("modules.VPNDetector.block-vpn", true);
        this.blockProxy = plugin.getConfig().getBoolean("modules.VPNDetector.block-proxy", true);
        this.riskThreshold = plugin.getConfig().getInt("modules.VPNDetector.risk-threshold", 66);
        this.blockedASNs = plugin.getConfig().getStringList("modules.VPNDetector.blocked-asns");
        
        if (vpnCache.size() > 1000) {
            vpnCache.clear();
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAsyncLogin(AsyncPlayerPreLoginEvent event) {
        if (!enabled)
            return;

        String ip = event.getAddress().getHostAddress();

        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1"))
            return;

        if (vpnCache.containsKey(ip)) {
            if (vpnCache.get(ip)) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        plugin.getLanguageManager().getMessage("modules.vpn-blocked"));
            }
            return;
        }

        boolean isVPN = checkVPN(ip);
        vpnCache.put(ip, isVPN);

        if (isVPN) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    plugin.getLanguageManager().getMessage("modules.vpn-blocked") + " &8(SEC-VPN-001)");
            plugin.getLogManager().logWarn("Blocked VPN/Proxy: " + ip);

            if (plugin.getDiscordWebhook() != null) {
                plugin.getDiscordWebhook().sendAlert("VPN Blocked",
                        "Blocked VPN/Proxy connection from IP: `" + ip + "`", 16755200);
            }
        }
    }

    private boolean checkVPN(String ip) {
        // Multi-Layer Strategy:
        // 1. ASN Blocking (Immediate, no external API call needed if cached/local)
        // 2. IP-API (Free, Fast, decent hosting detection)
        // 3. ProxyCheck.io (If key provided)
        // 4. IPHub (If key provided)
        
        boolean identified = false;
        
        // Layer 1: IP-API.com (Note: Free plan only supports HTTP, Pro supports HTTPS)
        try {
            URL url = java.net.URI.create("http://ip-api.com/json/" + ip + "?fields=proxy,hosting,mobile,status,isp,as").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String json = reader.readLine();
                if (json != null) {
                    // ASN Check from IP-API
                    if (blockedASNs != null && !blockedASNs.isEmpty()) {
                        for (String asn : blockedASNs) {
                            if (json.contains("\"as\":\"" + asn)) {
                                identified = true;
                                break;
                            }
                        }
                    }
                    
                    if (!identified) {
                        if (blockProxy && json.contains("\"proxy\":true")) identified = true;
                        if (blockVPN && (json.contains("\"vpn\":true") || json.contains("\"proxy\":true"))) identified = true;
                        if (blockHosting && json.contains("\"hosting\":true")) identified = true;
                        
                        // Risk Score Logic
                        int score = 0;
                        if (json.contains("\"proxy\":true")) score += 70;
                        if (json.contains("\"hosting\":true")) score += 40;
                        if (score >= riskThreshold) identified = true;
                    }
                }
            }
        } catch (java.io.IOException ignored) {}

        if (identified) return true;

        // Layer 2: ProxyCheck.io
        if (!proxyCheckKey.isEmpty()) {
            try {
                URL url = java.net.URI.create("http://proxycheck.io/v2/" + ip + "?key=" + proxyCheckKey + "&vpn=3").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2000);
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String json = reader.readLine();
                    if (json != null && (json.contains("\"proxy\":\"yes\"") || json.contains("\"type\":\"VPN\""))) {
                        identified = true;
                    }
                }
            } catch (java.io.IOException ignored) {}
        }

        if (identified) return true;

        // Layer 3: IPHub.info
        if (!ipHubKey.isEmpty()) {
            try {
                URL url = java.net.URI.create("https://v2.api.iphub.info/ip/" + ip).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("X-Key", ipHubKey);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2000);
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String json = reader.readLine();
                    if (json != null && (json.contains("\"block\":1") || json.contains("\"block\":2"))) {
                        identified = true;
                    }
                }
            } catch (java.io.IOException ignored) {}
        }

        return identified;
    }
}
