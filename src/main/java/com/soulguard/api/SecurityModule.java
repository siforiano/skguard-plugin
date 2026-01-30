package com.soulguard.api;

/**
 * Core interface for all SoulGuard security modules.
 * <p>
 * Modules are self-contained security features that can be enabled, disabled,
 * and reloaded independently. Each module should handle its own configuration,
 * event listeners, and cleanup.
 * </p>
 * 
 * <p><b>Example implementation:</b></p>
 * <pre>{@code
 * public class MySecurityModule implements SecurityModule {
 *     private boolean enabled;
 *     
 *     @Override
 *     public String getName() {
 *         return "MySecurityModule";
 *     }
 *     
 *     @Override
 *     public void enable() {
 *         this.enabled = true;
 *         // Initialize your module
 *     }
 *     
 *     @Override
 *     public void disable() {
 *         this.enabled = false;
 *         // Cleanup resources
 *     }
 * }
 * }</pre>
 * 
 * @author SoulGuard Team
 * @since 1.0
 */
public interface SecurityModule {
    
    /**
     * Gets the unique name of this module.
     * <p>
     * This name is used for configuration keys and logging.
     * It should be consistent across restarts.
     * </p>
     * 
     * @return the module name (e.g., "AuthModule", "AntiExploit")
     */
    String getName();
    
    /**
     * Checks if this module is currently enabled.
     * 
     * @return true if the module is active, false otherwise
     */
    boolean isEnabled();
    
    /**
     * Enables this module.
     * <p>
     * This method should initialize all resources, register event listeners,
     * and start any background tasks. It will be called automatically by
     * the ModuleManager if the module is enabled in config.yml.
     * </p>
     * 
     * @throws RuntimeException if initialization fails
     */
    void enable();
    
    /**
     * Disables this module.
     * <p>
     * This method should clean up all resources, unregister listeners,
     * and stop background tasks. It will be called automatically on
     * plugin shutdown or when the module is disabled via config.
     * </p>
     */
    void disable();
    
    /**
     * Reloads this module's configuration.
     * <p>
     * This method is called when the plugin configuration is reloaded.
     * The module should re-read its settings from config.yml without
     * fully disabling and re-enabling.
     * </p>
     */
    void reload();
}
