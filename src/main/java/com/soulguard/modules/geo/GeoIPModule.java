package com.soulguard.modules.geo;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GeoIPModule implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private List<String> blockedCountries;

    // Cache: IP -> Country Code
    private final Map<String, String> ipCache;
    private static final String API_URL = "http://ip-api.com/json/";

    public GeoIPModule(SoulGuard plugin) {
        this.plugin = plugin;
        this.ipCache = new ConcurrentHashMap<>();
        this.blockedCountries = new ArrayList<>();
    }

    @Override
    public String getName() {
        return "GeoIPModule";
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
        this.ipCache.clear();
    }

    @Override
    public void reload() {
        this.blockedCountries = plugin.getConfig().getStringList("modules.GeoIPModule.blocked-countries");

        if (ipCache.size() > 1000) {
            ipCache.clear();
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAsyncLogin(AsyncPlayerPreLoginEvent event) {
        if (!enabled || blockedCountries.isEmpty())
            return;

        String ip = event.getAddress().getHostAddress();

        // Skip localhost
        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1"))
            return;

        String countryCode = getCountryCode(ip);

        if (countryCode != null && blockedCountries.contains(countryCode.toUpperCase())) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    plugin.getLanguageManager().getMessage("modules.geoip-blocked"));
            plugin.getLogManager().logWarn("Blocked connection from " + countryCode + " (IP: " + ip + ")");

            if (plugin.getDiscordWebhook() != null) {
                plugin.getDiscordWebhook().sendAlert("GeoIP Block",
                        "Blocked connection from **" + countryCode + "** (IP: `" + ip + "`)", 16711680);
            }
        }
    }

    private String getCountryCode(String ip) {
        if (ipCache.containsKey(ip)) {
            return ipCache.get(ip);
        }

        try {
            URL url = new URL(API_URL + ip);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String json = response.toString();
            int index = json.indexOf("\"countryCode\":\"");
            if (index != -1) {
                String countryCode = json.substring(index + 15, index + 17);
                ipCache.put(ip, countryCode);
                return countryCode;
            }

        } catch (Exception e) {
            plugin.getLogManager().logError("GeoIP lookup failed for " + ip);
        }

        return null;
    }
}
