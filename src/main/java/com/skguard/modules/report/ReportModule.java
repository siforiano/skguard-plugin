package com.skguard.modules.report;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;
import com.skguard.util.ColorUtil;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReportModule implements SecurityModule {

    private final SKGuard plugin;
    private boolean enabled;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public ReportModule(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "ReportSystem";
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

    public void handleReport(Player reporter, String targetName, String reason) {
        if (!enabled)
            return;

        UUID uuid = reporter.getUniqueId();
        long now = System.currentTimeMillis();
        if (cooldowns.containsKey(uuid) && now - cooldowns.get(uuid) < 60000) {
            reporter.sendMessage(ColorUtil.translate("&c[SKGuard] Please wait before sending another report."));
            return;
        }

        cooldowns.put(uuid, now);

        // Log to audit
        plugin.getAuditManager().logAction(reporter, "PLAYER_REPORT", "Reported " + targetName + " for: " + reason);

        // Send to Discord
        if (plugin.getDiscordWebhook().isEnabled()) {
            String content = "🚨 **NEW REPORT** 🚨\n" +
                    "**Reporter:** " + reporter.getName() + " (" + reporter.getUniqueId() + ")\n" +
                    "**Target:** " + targetName + "\n" +
                    "**Reason:** " + reason + "\n" +
                    "**Location:** " + Math.round(reporter.getLocation().getX()) + ", " +
                    Math.round(reporter.getLocation().getY()) + ", " +
                    Math.round(reporter.getLocation().getZ());

            plugin.getDiscordWebhook().sendSimpleEmbed("Player Report", content, "FF0000");
        }

        reporter.sendMessage(ColorUtil.translate(
                "&a[SKGuard] Thank you! Your report against &e" + targetName + " &ahas been sent to our staff."));
    }
}

