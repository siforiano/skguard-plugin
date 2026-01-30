package com.soulguard.modules.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;

public class BruteForceProtector implements SecurityModule {

    private final SoulGuard plugin;
    private boolean enabled;
    private final Map<String, Integer> attempts = new ConcurrentHashMap<>();
    private final Map<String, Long> lockouts = new ConcurrentHashMap<>();

    private int maxAttempts;
    private long lockoutDuration; // in milliseconds

    public BruteForceProtector(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "BruteForceProtector";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        this.maxAttempts = plugin.getConfig().getInt("auth.brute-force.max-attempts", 5);
        this.lockoutDuration = plugin.getConfig().getLong("auth.brute-force.lockout-minutes", 15) * 60 * 1000;
    }

    @Override
    public void disable() {
        this.enabled = false;
        attempts.clear();
        lockouts.clear();
    }

    @Override
    public void reload() {
        enable();
    }

    public boolean isLocked(String ip) {
        if (!enabled) return false;
        if (lockouts.containsKey(ip)) {
            if (System.currentTimeMillis() > lockouts.get(ip)) {
                lockouts.remove(ip);
                attempts.remove(ip);
                return false;
            }
            return true;
        }
        return false;
    }

    public void recordFailure(String ip) {
        if (!enabled) return;
        int count = attempts.getOrDefault(ip, 0) + 1;
        attempts.put(ip, count);

        if (count >= maxAttempts) {
            lockouts.put(ip, System.currentTimeMillis() + lockoutDuration);
            plugin.getLogManager().logWarn("[Security] IP locked for brute force: " + ip);
        }
    }

    public void recordSuccess(String ip) {
        attempts.remove(ip);
        lockouts.remove(ip);
    }
}
