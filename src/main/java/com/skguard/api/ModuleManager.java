package com.skguard.api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import com.skguard.SKGuard;

/**
 * Manages the lifecycle of all security modules in SKGuard.
 * <p>
 * This class handles registration, enabling, disabling, and reloading of modules.
 * Modules are organized into groups for better categorization and can be
 * accessed by name or class type.
 * </p>
 * 
 * @author SKGuard Team
 * @since 1.0
 */
public class ModuleManager {

    private final SKGuard plugin;
    private final Map<String, SecurityModule> modules;
    private final Map<Class<? extends SecurityModule>, SecurityModule> moduleClasses;
    private final Map<ModuleGroup, java.util.List<SecurityModule>> moduleGroups;

    public ModuleManager(SKGuard plugin) {
        this.plugin = plugin;
        this.modules = new ConcurrentHashMap<>();
        this.moduleClasses = new ConcurrentHashMap<>();
        this.moduleGroups = new ConcurrentHashMap<>();
    }

    /**
     * Registers a security module with the manager.
     * <p>
     * The module will be automatically enabled if it's enabled in config.yml.
     * Modules are registered by name, class, and group for flexible access.
     * </p>
     * 
     * @param group the module group (e.g., IDENTITY, ANTICHEAT)
     * @param module the module instance to register
     */
    public void registerModule(ModuleGroup group, SecurityModule module) {
        modules.put(module.getName(), module);
        moduleClasses.put(module.getClass(), module);
        moduleGroups.computeIfAbsent(group, k -> new java.util.ArrayList<>()).add(module);
        
        boolean enabledInConfig = plugin.getConfig().getBoolean("modules." + module.getName() + ".enabled", true);

        if (enabledInConfig) {
            enableModule(module);
            com.skguard.util.ConsoleUtil.moduleLoad(module.getName(), true);
        } else {
            com.skguard.util.ConsoleUtil.moduleLoad(module.getName(), false);
        }
    }

    /**
     * Gets all modules in a specific group.
     * 
     * @param group the module group
     * @return list of modules in the group, or empty list if none
     */
    public java.util.List<SecurityModule> getModulesInGroup(ModuleGroup group) {
        return moduleGroups.getOrDefault(group, java.util.Collections.emptyList());
    }

    /**
     * Gets a module by its class type.
     * <p>
     * This is the type-safe way to retrieve modules.
     * </p>
     * 
     * @param <T> the module type
     * @param clazz the module class
     * @return the module instance, or null if not found
     */
    public <T extends SecurityModule> T getModule(Class<T> clazz) {
        SecurityModule module = moduleClasses.get(clazz);
        return module != null ? clazz.cast(module) : null;
    }

    public void enableModule(SecurityModule module) {
        try {
            module.enable();
            if (module instanceof Listener listener) {
                plugin.getServer().getPluginManager().registerEvents(listener, plugin);
            }
        } catch (Exception e) {
            plugin.getLogManager().logError("Failed to enable module " + module.getName() + ": " + e.getMessage());
        }
    }

    public void disableModule(SecurityModule module) {
        try {
            module.disable();
            if (module instanceof Listener listener) {
                HandlerList.unregisterAll(listener);
            }
            plugin.getLogManager().logInfo("Disabled module: " + module.getName());
        } catch (Exception e) {
            plugin.getLogManager().logError("Failed to disable module " + module.getName() + ": " + e.getMessage());
        }
    }

    public void reloadModules() {
        for (SecurityModule module : modules.values()) {
            boolean enabledInConfig = plugin.getConfig().getBoolean("modules." + module.getName() + ".enabled", true);
            if (enabledInConfig && !module.isEnabled()) {
                enableModule(module);
            } else if (!enabledInConfig && module.isEnabled()) {
                disableModule(module);
            }
            if (module.isEnabled()) {
                module.reload();
            }
        }
    }

    /**
     * Gets a module by its name.
     * 
     * @param name the module name
     * @return the module instance, or null if not found
     */
    public SecurityModule getModule(String name) {
        return modules.get(name);
    }

    /**
     * Gets the group a module belongs to.
     * 
     * @param module the module instance
     * @return the module group, or null if not found
     */
    public ModuleGroup getModuleGroup(SecurityModule module) {
        for (Map.Entry<ModuleGroup, java.util.List<SecurityModule>> entry : moduleGroups.entrySet()) {
            if (entry.getValue().contains(module)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Gets all registered modules.
     * 
     * @return collection of all modules
     */
    public java.util.Collection<SecurityModule> getModules() {
        return modules.values();
    }
}

