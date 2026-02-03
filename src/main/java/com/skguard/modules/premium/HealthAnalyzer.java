package com.skguard.modules.premium;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;
import com.skguard.util.ColorUtil;

/**
 * Premium Elite Feature: Security Health Score.
 * Analyzes the current configuration and active modules to provide 
 * a comprehensive 'Health Score' (0-100%) with actionable advice.
 */
public class HealthAnalyzer implements SecurityModule {

    private final SKGuard plugin;
    private boolean enabled;

    public HealthAnalyzer(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "HealthAnalyzer";
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
    public void reload() {}

    public int calculateScore(List<String> advice) {
        int score = 0;
        int totalPossible = 0;

        // 1. Core Modules Check (Weight: 30)
        totalPossible += 30;
        if (isModuleActive("AuthModule")) score += 15;
        else advice.add("&e- AuthModule is disabled. (High Risk)");
        
        if (isModuleActive("NeoGuard")) score += 15;
        else advice.add("&e- NeoGuard AntiCheat is disabled. (High Risk)");

        // 2. Elite Features Check (Weight: 40)
        totalPossible += 40;
        if (isModuleActive("BotFirewall")) score += 10;
        else advice.add("&7- Bot Firewall is not active. (Medium Risk)");
        
        if (isModuleActive("ShadowMirror")) score += 10;
        else advice.add("&7- Shadow Ban Mirror is disabled.");
        
        if (isModuleActive("ThreatAnalyzer")) score += 20;
        else advice.add("&6- Threat Analyzer (Core Brain) is disabled.");

        // 3. Configuration Safety (Weight: 30)
        totalPossible += 30;
        if (plugin.getConfig().getBoolean("modules.StaffPIN.pattern-pin", true)) score += 15;
        else advice.add("&b- Sequential PIN Pattern is disabled for Staff.");
        
        if (plugin.getDatabaseManager().isMySQL()) score += 15;
        else advice.add("&b- Using Flat-File for storage. Redis/MySQL recommended.");

        return (int) ((score / (double) totalPossible) * 100);
    }

    private boolean isModuleActive(String name) {
        SecurityModule mod = plugin.getModuleManager().getModule(name);
        return mod != null && mod.isEnabled();
    }

    public void sendReport(Player player) {
        List<String> advice = new ArrayList<>();
        int score = calculateScore(advice);
        
        String color = score > 80 ? "&a" : (score > 50 ? "&e" : "&c");
        
        player.sendMessage(ColorUtil.translate("&8&l&m----------------------------------------"));
        player.sendMessage(ColorUtil.translate("&6&lSKGuard &f» &bSecurity Health Report"));
        player.sendMessage(ColorUtil.translate(""));
        player.sendMessage(ColorUtil.translate("&7Global Score: " + color + "&l" + score + "%"));
        player.sendMessage(ColorUtil.translate(""));
        
        if (advice.isEmpty()) {
            player.sendMessage(ColorUtil.translate("&a✔ Your server security is optimal."));
        } else {
            player.sendMessage(ColorUtil.translate("&fRecommendations:"));
            for (String line : advice) {
                player.sendMessage(ColorUtil.translate(line));
            }
        }
        player.sendMessage(ColorUtil.translate("&8&l&m----------------------------------------"));
    }
}

