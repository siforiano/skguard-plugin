package com.soulguard.api;

/**
 * Categories for organizing security modules.
 * 
 * @author SoulGuard Team
 * @since 1.0
 */
public enum ModuleGroup {
    /** Staff protection and security modules */
    STAFF("Staff & Admin Security"),
    /** Identity, authentication, and account security */
    IDENTITY("Identity & Account Protection"),
    /** Advanced defense mechanisms */
    DEFENSE("Advanced Defense"),
    /** Anti-cheat detection systems */
    ANTICHEAT("Anti-Cheat (Neo-Guard)"),
    /** Anti-exploit and protection */
    EXPLOIT("Anti-Exploit & Technical"),
    /** Social features and moderation */
    SOCIAL("Social & Moderation"),
    /** System utilities and core features */
    SYSTEM("Server Management"),
    /** Premium-only exclusive features */
    PREMIUM("Premium Features"),
    /** Custom third-party modules */
    CUSTOM("Custom Modules");

    private final String displayName;

    ModuleGroup(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
