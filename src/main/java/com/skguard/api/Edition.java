package com.skguard.api;

/**
 * Represents the edition of SKGuard being used.
 * 
 * @author SKGuard Team
 * @since 1.0
 */
public enum Edition {
    /**
     * Free version with all current features, unlimited players.
     * Does not include premium-only features like Web Panel, ML Detection, etc.
     */
    LITE,
    
    /**
     * Premium version with all features including exclusive advanced capabilities.
     * Includes Web Panel, Machine Learning, Cloud Blacklist, Forensics, and more.
     */
    PREMIUM;
    
    /**
     * Checks if this edition is the premium version.
     * 
     * @return true if this is the premium edition
     */
    public boolean isPremium() {
        return this == PREMIUM;
    }
    
    /**
     * Checks if this edition is the lite (free) version.
     * 
     * @return true if this is the lite edition
     */
    public boolean isLite() {
        return this == LITE;
    }
    
    /**
     * Gets a user-friendly display name for this edition.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return this == PREMIUM ? "Premium" : "Lite";
    }
}

