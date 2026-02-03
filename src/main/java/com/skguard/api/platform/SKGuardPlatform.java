package com.skguard.api.platform;

import java.util.UUID;

/**
 * Common interface for platform-specific operations (Bukkit, Velocity).
 */
public interface SKGuardPlatform {
    
    void sendMessage(UUID uuid, String message);
    
    void kickPlayer(UUID uuid, String reason);
    
    boolean hasPermission(UUID uuid, String permission);
    
    String getPlatformName();
    
    boolean isOnline(UUID uuid);
    
    void runAsync(Runnable runnable);
    
    void runSync(Runnable runnable);
}

