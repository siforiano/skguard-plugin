package com.skguard.modules.system;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;
import com.skguard.modules.auth.AuthModule;
import com.skguard.modules.auth.FastLoginEngine;
import com.skguard.modules.auth.TOTPManager;

public class ConflictManager implements SecurityModule {

    private final SKGuard plugin;
    private boolean enabled;

    public ConflictManager(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "ConflictManager";
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
        // No config to reload for now
    }

    public List<String> checkConflicts() {
        List<String> conflicts = new ArrayList<>();

        // 1. External Plugin Conflicts
        String[] conflictingPlugins = { "AuthMe", "nLogin", "LoginSecurity", "OpenLogin", "UserLogin", "xAuth", "CrazyLogin" };
        
        AuthModule authModule = plugin.getModule(AuthModule.class);
        boolean isAuthEnabled = authModule != null && authModule.isEnabled();

        for (String plName : conflictingPlugins) {
            Plugin pl = Bukkit.getPluginManager().getPlugin(plName);
            if (pl != null && pl.isEnabled()) {
                if (isAuthEnabled) {
                    conflicts.add("&c[CRITICAL] Competing authentication plugin detected: &e" + plName + "&c. Disable SKGuard's AuthModule or the other plugin to avoid conflicts.");
                } else {
                    conflicts.add("&e[INFO] External auth plugin detected: &f" + plName + "&e. SKGuard AuthModule is correctly disabled.");
                }
            }
        }

        // 2. Internal Consistency Checks
        if (!isAuthEnabled) {
            FastLoginEngine fastLogin = plugin.getModule(FastLoginEngine.class);
            if (fastLogin != null && fastLogin.isEnabled()) {
                conflicts.add("&6[WARN] FastLoginEngine is enabled but AuthModule is disabled. It will not function.");
            }

            TOTPManager totp = plugin.getModule(TOTPManager.class);
            if (totp != null && totp.isEnabled()) {
                conflicts.add("&6[WARN] TOTPManager (2FA) is enabled but AuthModule is disabled. It will not function.");
            }
        }

        // 3. Server Mode Check
        if (Bukkit.getOnlineMode() && isAuthEnabled) {
            conflicts.add("&6[WARN] Server is in online-mode=true but AuthModule is enabled. This is usually redundant unless for specific cracked logic support.");
        }

        if (conflicts.isEmpty()) {
            conflicts.add("&a[OK] No active conflicts detected.");
        }

        return conflicts;
    }
}

