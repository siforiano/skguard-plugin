package com.skguard.util;

import org.bukkit.configuration.file.FileConfiguration;

import com.skguard.SKGuard;

public class SecurityPresets {

    public enum Preset {
        LOW, STANDARD, STRICT, PARANOID
    }

    public static void applyPreset(SKGuard plugin, Preset preset) {
        FileConfiguration config = plugin.getConfig();

        switch (preset) {
            case LOW -> {
                config.set("modules.VPNDetector.risk-threshold", 85);
                config.set("modules.anticheat.intensity", 3);
                config.set("modules.AI_BotFirewall.mitigation-level", 1);
                config.set("modules.AuthModule.session-minutes", 60);
            }
            case STANDARD -> {
                config.set("modules.VPNDetector.risk-threshold", 66);
                config.set("modules.anticheat.intensity", 5);
                config.set("modules.AI_BotFirewall.mitigation-level", 3);
                config.set("modules.AuthModule.session-minutes", 30);
            }
            case STRICT -> {
                config.set("modules.VPNDetector.risk-threshold", 50);
                config.set("modules.anticheat.intensity", 8);
                config.set("modules.AI_BotFirewall.mitigation-level", 4);
                config.set("modules.AuthModule.session-minutes", 10);
                config.set("modules.StaffSecurity.ip-drift-lock", true);
            }
            case PARANOID -> {
                config.set("modules.VPNDetector.risk-threshold", 30);
                config.set("modules.anticheat.intensity", 10);
                config.set("modules.AI_BotFirewall.mitigation-level", 5);
                config.set("modules.AI_BotFirewall.human-verification", true);
                config.set("modules.AuthModule.session-minutes", 0);
                config.set("modules.StaffSecurity.ip-drift-lock", true);
                config.set("modules.CaptchaModule.on-first-join", true);
            }
        }
        
        plugin.saveConfig();
        plugin.getModuleManager().reloadModules();
    }
}

