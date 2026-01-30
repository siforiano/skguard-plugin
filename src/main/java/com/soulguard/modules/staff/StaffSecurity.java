package com.soulguard.modules.staff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;

public class StaffSecurity implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private final Map<String, String> staffIps; // Username -> IP
    private boolean enableIpWhitelist;

    public StaffSecurity(SoulGuard plugin) {
        this.plugin = plugin;
        this.staffIps = new HashMap<>();
        this.enableIpWhitelist = true;
    }

    @Override
    public String getName() {
        return "StaffSecurity";
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
        this.staffIps.clear();
    }

    @Override
    public void reload() {
        this.staffIps.clear();
        this.enableIpWhitelist = plugin.getConfig().getBoolean("modules.StaffSecurity.ip-whitelist.enabled", true);

        List<String> staffList = plugin.getConfig().getStringList("modules.StaffSecurity.ip-whitelist.accounts");
        for (String entry : staffList) {
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                this.staffIps.put(parts[0].toLowerCase(), parts[1]);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAsyncLogin(AsyncPlayerPreLoginEvent event) {
        if (!enabled || !enableIpWhitelist)
            return;

        String name = event.getName().toLowerCase();
        String ip = event.getAddress().getHostAddress();

        // If player is in the staff configuration
        if (staffIps.containsKey(name)) {
            String allowedIp = staffIps.get(name);
            if (!ip.equals(allowedIp)) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                        ChatColor.RED + "Security Alert: You are joining from an unauthorized IP.");

                plugin.getLogManager().logError("Staff " + name + " tried to join from unauthorized IP: " + ip);
                if (plugin.getDiscordWebhook() != null) {
                    plugin.getDiscordWebhook().sendAlert("Staff Security",
                            "**SECURITY ALERT**: Staff **" + name + "** tried to join from unauthorized IP `" + ip
                                    + "` (Expected: `" + allowedIp + "`)",
                            16711680);
                }
            }
        }
    }
}
