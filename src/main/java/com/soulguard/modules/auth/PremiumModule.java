package com.soulguard.modules.auth;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;

public class PremiumModule implements SecurityModule {

    private final SoulGuard plugin;
    private boolean enabled;
    private final Set<UUID> premiumUsers = new HashSet<>();

    public PremiumModule(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "PremiumVerification";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        loadPremiumData();
    }

    @Override
    public void disable() {
        this.enabled = false;
        premiumUsers.clear();
    }

    @Override
    public void reload() {
        loadPremiumData();
    }

    private void loadPremiumData() {
        premiumUsers.clear();
        if (plugin.getConfig().contains("premium-users")) {
            for (String uuidStr : plugin.getConfig().getStringList("premium-users")) {
                try {
                    premiumUsers.add(UUID.fromString(uuidStr));
                } catch (Exception ignored) {}
            }
        }
    }

    public boolean isPremium(UUID uuid) {
        return premiumUsers.contains(uuid);
    }

}
