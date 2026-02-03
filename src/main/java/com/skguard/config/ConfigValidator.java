package com.skguard.config;

import java.util.ArrayList;
import java.util.List;

import com.skguard.SKGuard;

/**
 * Premium Elite Feature: Config Pro-Validator.
 * Scans the plugin configuration on startup for dangerous values, 
 * performance bottlenecks, or inconsistent settings.
 */
public class ConfigValidator {

    private final SKGuard plugin;

    public ConfigValidator(SKGuard plugin) {
        this.plugin = plugin;
    }

    public void validate() {
        List<String> warnings = new ArrayList<>();

        // 1. Dangerous Thresholds
        if (plugin.getConfig().getDouble("modules.NeoGuard.checks.Reach.threshold", 3.0) < 3.0) {
            warnings.add("NeoGuard Reach threshold is extremely low (< 3.0). High risk of False Positives.");
        }

        // 2. Performance Bottlenecks
        if (plugin.getConfig().getBoolean("general.debug", false)) {
            warnings.add("Debug mode is ENABLED. This may impact performance in production environments.");
        }

        // 3. Security Blind Spots
        if (!plugin.getConfig().getBoolean("modules.StaffPIN.enabled", true)) {
            warnings.add("StaffPIN is DISABLED. Staff accounts are highly vulnerable to session hijacking.");
        }

        if (warnings.isEmpty()) {
            plugin.getLogManager().logInfo("[Validator] Configuration analysis complete: 0 issues found.");
        } else {
            plugin.getLogManager().logWarn("[Validator] analysis found " + warnings.size() + " potential issues:");
            for (String warn : warnings) {
                plugin.getLogManager().logWarn("  ! " + warn);
            }
        }
    }
}

