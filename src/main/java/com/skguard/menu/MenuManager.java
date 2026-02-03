package com.skguard.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;
import com.skguard.util.ColorUtil;

public class MenuManager implements Listener {

    private final SKGuard plugin;
    private static final String MENU_TITLE = ColorUtil.translate("&8&lSKGuard &7» &fConfig");
    private static final String PRESETS_TITLE = ColorUtil.translate("&8&lSKGuard &7» &ePresets");
    private static final String DASHBOARD_TITLE = ColorUtil.translate("&8&lSKGuard &7» &bDashboard");
    private static final String LOGS_TITLE = ColorUtil.translate("&8&lSKGuard &7» &fAudit Logs");
    private final Map<String, Material> moduleIcons = new HashMap<>();
    private final Map<String, String> moduleDescriptions = new HashMap<>();

    public MenuManager(SKGuard plugin) {
        if (plugin == null)
            throw new IllegalArgumentException("Plugin cannot be null");
        this.plugin = plugin;
        setupIcons();
    }

    private void setupIcons() {
        moduleIcons.put("AuthModule", Material.IRON_DOOR);
        moduleIcons.put("StaffPIN", Material.GOLD_INGOT);
        moduleIcons.put("CaptchaModule", Material.COMPASS);
        moduleIcons.put("AntiExploit", Material.BARRIER);
        moduleIcons.put("ChatFilter", Material.PAPER);
        moduleIcons.put("ConnectionManager", Material.BEACON);
        moduleIcons.put("WhitelistManager", Material.WRITABLE_BOOK);
        moduleIcons.put("VPNDetector", Material.SPYGLASS);
        moduleIcons.put("PanicMode", Material.TNT);
        moduleIcons.put("Maintenance", Material.ANVIL);
        moduleIcons.put("Honeypot", Material.SPIDER_EYE);
        moduleIcons.put("PacketInspector", Material.OBSERVER);
        moduleIcons.put("ReportSystem", Material.BOOK);
        moduleIcons.put("IdentityGuard", Material.NAME_TAG);
        moduleIcons.put("AutoPunish", Material.NETHERITE_AXE);
        moduleIcons.put("DiscordVerify", Material.REPEATER);
        moduleIcons.put("ShadowBan", Material.WITHER_SKELETON_SKULL);
        moduleIcons.put("TravelGuard", Material.MAP);
        moduleIcons.put("CommandGuard", Material.BARRIER);
        moduleIcons.put("StaffAnalytics", Material.CHEST_MINECART);
        moduleIcons.put("StaffQuarantine", Material.IRON_BARS);
        moduleIcons.put("ScamGuard", Material.SKELETON_SKULL);
        moduleIcons.put("StaffRateLimit", Material.CLOCK);
        moduleIcons.put("UserSecurity", Material.IRON_DOOR);
        moduleIcons.put("PrivacyGuard", Material.PAPER);
        moduleIcons.put("TradeGuard", Material.EMERALD);
        moduleIcons.put("GhostGuard", Material.ENDER_EYE);
        moduleIcons.put("BehaviorMonitor", Material.OBSERVER);
        moduleIcons.put("AltLinker", Material.TRIPWIRE_HOOK);
        moduleIcons.put("RollbackManager", Material.CLOCK);
        moduleIcons.put("GhostCommands", Material.BARRIER);
        moduleIcons.put("CommandFirewall", Material.FIRE_CHARGE);
        moduleIcons.put("InventorySnapshots", Material.MAP);
        moduleIcons.put("NeoGuard", Material.NETHERITE_SWORD);

        moduleDescriptions.put("AuthModule", "Essential login & registration security.");
        moduleDescriptions.put("StaffPIN", "Secondary protection for staff members.");
        moduleDescriptions.put("CaptchaModule", "Interruption-based bot verification.");
        moduleDescriptions.put("AntiExploit", "Protects against common server exploits.");
        moduleDescriptions.put("PanicMode", "Emergency lockdown for all players.");
        moduleDescriptions.put("Maintenance", "Restrict server access to staff only.");
        moduleDescriptions.put("Honeypot", "Trap for bots using invisible entities.");
        moduleDescriptions.put("PacketInspector", "Anti-crash and technical packet monitoring.");
        moduleDescriptions.put("ReportSystem", "Integrated /report system with Discord alerts.");
        moduleDescriptions.put("IdentityGuard", "UUID Spoof Guard and Anti-Book Exploit.");
        moduleDescriptions.put("AutoPunish", "Score-based automated player punishments.");
        moduleDescriptions.put("DiscordVerify", "Linked Discord secondary verification for staff.");
        moduleDescriptions.put("ShadowBan", "Isolate players without their knowledge.");
        moduleDescriptions.put("TravelGuard", "Impossible geography jump detection.");
        moduleDescriptions.put("CommandGuard", "Hide tab-completions until verified.");
        moduleDescriptions.put("StaffAnalytics", "Track and display staff performance.");
        moduleDescriptions.put("StaffQuarantine", "Strip powers until full verification.");
        moduleDescriptions.put("ScamGuard", "Proactive scam and impersonation detection.");
        moduleDescriptions.put("StaffRateLimit", "Prevent mass destructive actions.");
        moduleDescriptions.put("UserSecurity", "Session lock and login alerts for players.");
        moduleDescriptions.put("PrivacyGuard", "PII shield and phishing blocker.");
        moduleDescriptions.put("TradeGuard", "High-value drop confirmation.");
        moduleDescriptions.put("GhostGuard", "Bot & KillAura detection traps.");
        moduleDescriptions.put("BehaviorMonitor", "Atypical behavior & pattern analysis.");
        moduleDescriptions.put("AltLinker", "Connect and track multiple accounts.");
        moduleDescriptions.put("RollbackManager", "Emergency staff action reversal.");
        moduleDescriptions.put("GhostCommands", "Fake admin commands to trap intruders.");
        moduleDescriptions.put("CommandFirewall", "Context-aware staff command blocking.");
        moduleDescriptions.put("InventorySnapshots", "Auto-save player state on security alerts.");
        moduleDescriptions.put("NeoGuard", "Advanced predictive AntiCheat system.");
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MENU_TITLE);

        // Border & Design
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i > 44 || i % 9 == 0 || (i + 1) % 9 == 0) {
                inv.setItem(i, border);
            }
        }

        // Header
        String platformInfo = com.skguard.util.BedrockManager.getPlatform(player);
        inv.setItem(4, createItem(Material.NETHER_STAR, "&6&lSKGuard Central Control",
                "&7Manage all security systems from here.",
                "&7Client: " + platformInfo,
                "&7Version: &e" + plugin.getEdition().getDisplayName(),
                "&7Status: &aSystem Healthy"));

        // Modules starting at row 2
        com.skguard.api.ModuleManager mm = plugin.getModuleManager();
        if (mm != null) {
            int slot = 10;
            for (SecurityModule module : mm.getModules()) {
                if (slot == 17)
                    slot = 19;
                if (slot == 26)
                    slot = 28;
                if (slot == 35)
                    slot = 37;

                inv.setItem(slot++, createModuleItem(module));

                if (slot > 43)
                    break;
            }
        }

        // Feature Bar (Row 6)
        inv.setItem(48, createItem(Material.WRITABLE_BOOK, "&b&lSecurity Audit Logs",
                "&7View real-time security events.", "&eLeft-Click to open viewer", "&bRight-Click for Dashboard"));
        inv.setItem(49, createItem(Material.SHIELD, "&e&lSecurity Presets", 
                "&7Quickly adjust security intensity.", "&bClick to select perfiles"));
        inv.setItem(50, createItem(Material.GLOW_ITEM_FRAME, "&d&lSystem Diagnostics", "&7Run a full system check.",
                "&cDetect conflicts & issues."));

        // Logs Entry
        inv.setItem(31, createItem(Material.WRITABLE_BOOK, "&b&lQuick Activity", "&7Overview of recent security",
                "&7activity and heartbeat."));

        player.openInventory(inv);
    }

    private ItemStack createModuleItem(SecurityModule module) {
        boolean enabled = module.isEnabled();
        boolean isPremiumModule = plugin.getModuleManager()
                .getModuleGroup(module) == com.skguard.api.ModuleGroup.PREMIUM;
        boolean editionIsPremium = plugin.getEdition().isPremium();

        Material mat = moduleIcons.getOrDefault(module.getName(), Material.BOOK);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String prefix = isPremiumModule ? "&6[⭐] " : "&b[⚓] ";
            meta.setDisplayName(ColorUtil.translate(prefix + "&l" + module.getName()));

            List<String> lore = new ArrayList<>();
            if (isPremiumModule && !editionIsPremium) {
                lore.add(ColorUtil.translate("&c&lPREMIUM FEATURE"));
                lore.add(ColorUtil.translate("&7Please upgrade to unlock this system."));
                lore.add("");
            }

            lore.add(ColorUtil.translate(
                    "&7" + moduleDescriptions.getOrDefault(module.getName(), "Security enhancement module.")));
            lore.add("");
            lore.add(
                    ColorUtil.translate("&fStatus: " + (enabled ? "&8[&a ✔ &8] &aENABLED" : "&8[&c ✖ &8] &cDISABLED")));

            if (isPremiumModule && !editionIsPremium) {
                lore.add("");
                lore.add(ColorUtil.translate("&8Locked for Lite edition."));
            } else {
                lore.add("");
                lore.add(ColorUtil.translate("&eLeft-Click &7to " + (enabled ? "&cdisable" : "&aenable")));
                lore.add(ColorUtil.translate("&bRight-Click &7to &fconfigure adjustments"));
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void openModuleSettings(Player player, String moduleName) {
        String path = "modules." + moduleName;
        openConfigSection(player, moduleName, path);
    }

    private void openConfigSection(Player player, String title, String path) {
        Inventory inv = Bukkit.createInventory(null, 54, ColorUtil.translate("&8Settings » &e" + title));

        // Border
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i > 44 || i % 9 == 0 || (i + 1) % 9 == 0) {
                inv.setItem(i, border);
            }
        }

        int slot = 10;
        org.bukkit.configuration.ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (slot == 17)
                    slot = 19;
                if (slot == 26)
                    slot = 28;
                if (slot == 35)
                    slot = 37;

                Object val = section.get(key);
                if (val instanceof org.bukkit.configuration.ConfigurationSection) {
                    inv.setItem(slot++, createFolderItem(key, path + "." + key));
                } else if (val instanceof Boolean b) {
                    inv.setItem(slot++, createSettingItem(key, b, path + "." + key));
                } else if (val instanceof Number n) {
                    inv.setItem(slot++, createNumberItem(key, n, path + "." + key));
                }

                if (slot > 43)
                    break;
            }
        }

        inv.setItem(49, createItem(Material.ARROW, "&cBack", "&7Return to previous menu."));
        player.openInventory(inv);
    }

    private ItemStack createFolderItem(String key, String fullPath) {
        return createItem(Material.CHEST, "&6&l📁 " + key, "&7Click to open sub-settings.", "&8Path: " + fullPath);
    }

    private ItemStack createNumberItem(String key, Number value, String fullPath) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.translate("&e" + key));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.translate("&7Value: &b" + value));
            lore.add(ColorUtil.translate("&8Path: " + fullPath));
            lore.add("");
            lore.add(ColorUtil.translate("&eLeft-Click &7to &a+1"));
            lore.add(ColorUtil.translate("&bRight-Click &7to &c-1"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSettingItem(String key, boolean value, String fullPath) {
        ItemStack item = new ItemStack(value ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.translate("&e" + key));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.translate("&7Value: " + (value ? "&a✔ ON" : "&c✖ OFF")));
            lore.add(ColorUtil.translate("&8Path: " + fullPath));
            lore.add("");
            lore.add(ColorUtil.translate("&eClick to toggle."));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.translate(name));
            List<String> l = new ArrayList<>();
            for (String line : lore) {
                if (line != null)
                    l.add(ColorUtil.translate(line));
            }
            meta.setLore(l);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void openLogViewer(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, LOGS_TITLE);

        // Border
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++) {
            if (i < 9 || i > 26 || i % 9 == 0 || (i + 1) % 9 == 0) {
                inv.setItem(i, border);
            }
        }

        List<String> logs = plugin.getAuditManager().getRecentLogs(14);
        int slot = 10;
        for (String log : logs) {
            if (slot == 17)
                slot = 19;
            inv.setItem(slot++, createLogItem(log));
            if (slot >= 26)
                break;
        }

        inv.setItem(31, createItem(Material.ARROW, "&cBack to Menu", "&7Return to main overview."));
        player.openInventory(inv);
    }

    public void openPresetsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, PRESETS_TITLE);
        
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i > 17 || i == 9 || i == 17) inv.setItem(i, border);
        }

        inv.setItem(10, createItem(Material.WHITE_WOOL, "&f&lLOW", "&7Basic protection.", "&7High compatibility."));
        inv.setItem(12, createItem(Material.LIME_WOOL, "&a&lSTANDARD", "&7Balanced security.", "&e(Recommended)"));
        inv.setItem(14, createItem(Material.ORANGE_WOOL, "&6&lSTRICT", "&7High security.", "&cStrict monitoring."));
        inv.setItem(16, createItem(Material.RED_WOOL, "&c&lPARANOID", "&4&lMAXIMUM SECURITY", "&7Possible false positives."));

        inv.setItem(22, createItem(Material.ARROW, "&cBack", "&7Return to menu."));
        player.openInventory(inv);
    }

    public void openDashboard(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, DASHBOARD_TITLE);
        
        List<String> logs = plugin.getAuditManager().getRecentLogs(100);
        int warnings = (int) logs.stream().filter(l -> l.contains("WARN")).count();
        int errors = (int) logs.stream().filter(l -> l.contains("ERROR")).count();
        int info = (int) logs.stream().filter(l -> l.contains("INFO")).count();

        ItemStack border = createItem(Material.CYAN_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 27; i < 36; i++) inv.setItem(i, border);

        inv.setItem(11, createItem(Material.GOLD_BLOCK, "&e&lWarnings (1h)", "&7Found &f" + warnings + " &7security triggers."));
        inv.setItem(13, createItem(Material.REDSTONE_BLOCK, "&c&lErrors/Blocks (1h)", "&7Found &f" + errors + " &7critical blocks."));
        inv.setItem(15, createItem(Material.EMERALD_BLOCK, "&a&lSystem Uptime", "&7Heartbeat: &aHealthy", "&7Events processed: &f" + info));

        inv.setItem(31, createItem(Material.ARROW, "&cBack", "&7Return to menu."));
        player.openInventory(inv);
    }

    private ItemStack createLogItem(String log) {
        Material mat = Material.LIME_DYE;
        String color = "&a";
        if (log.contains("WARN")) {
            mat = Material.ORANGE_DYE;
            color = "&e";
        } else if (log.contains("ERROR")) {
            mat = Material.RED_DYE;
            color = "&c";
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String time = log.substring(0, Math.min(log.length(), 10)); // Approximate time
            meta.setDisplayName(ColorUtil.translate(color + "Activity Log at " + time));
            List<String> lore = new ArrayList<>();
            // Wrap text for lore
            String content = log.substring(Math.min(log.length(), 11));
            lore.add(ColorUtil.translate("&7" + (content.length() > 40 ? content.substring(0, 40) + "..." : content)));
            if (content.length() > 40) {
                lore.add(ColorUtil.translate("&7" + content.substring(40, Math.min(content.length(), 80))));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(MENU_TITLE) && !title.startsWith(ColorUtil.translate("&8Settings »"))
                && !title.equals(LOGS_TITLE))
            return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR
                || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null)
            return;

        if (title.equals(MENU_TITLE)) {
            handleMainClick(player, event);
        } else if (title.equals(PRESETS_TITLE)) {
            handlePresetsClick(player, event);
        } else if (title.equals(DASHBOARD_TITLE)) {
            if (clicked.getType() == Material.ARROW) openMainMenu(player);
        } else if (title.equals(LOGS_TITLE)) {
            if (clicked.getType() == Material.ARROW) {
                openMainMenu(player);
            }
        } else {
            handleSettingsClick(player, event);
        }
    }

    private void handleMainClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null)
            return;
        if (clicked.getType() == Material.NETHER_STAR)
            return;
        if (clicked.getType() == Material.WRITABLE_BOOK) {
            if (event.isLeftClick()) openLogViewer(player);
            else openDashboard(player);
            return;
        }
        if (clicked.getType() == Material.SHIELD) {
            openPresetsMenu(player);
            return;
        }
        if (clicked.getType() == Material.GLOW_ITEM_FRAME) {
            player.closeInventory();
            com.skguard.modules.system.ConflictManager cm = plugin.getModule(com.skguard.modules.system.ConflictManager.class);
            if (cm != null) {
                player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
                player.sendMessage(ColorUtil.translate("&6&lSKGuard System Diagnostics"));
                player.sendMessage("");
                for (String line : cm.checkConflicts()) {
                    player.sendMessage(ColorUtil.translate(line));
                }
                player.sendMessage(ColorUtil.translate("&8&m----------------------------------------"));
            } else {
                player.sendMessage(ColorUtil.translate("&cConflictManager is not enabled."));
            }
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null)
            return;
        String moduleName = ChatColor.stripColor(meta.getDisplayName());
        com.skguard.api.ModuleManager mm = plugin.getModuleManager();
        if (mm == null)
            return;
        SecurityModule module = mm.getModule(moduleName);

        if (module != null) {
            if (event.isLeftClick()) {
                boolean currentState = module.isEnabled();
                plugin.getConfig().set("modules." + module.getName() + ".enabled", !currentState);
                CompletableFuture.runAsync(() -> {
                    plugin.saveConfig();
                    plugin.getModuleManager().reloadModules();
                }).thenRun(() -> {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        openMainMenu(player);
                        player.sendMessage(ColorUtil.translate(
                                "&a[SKGuard] &f" + moduleName + " &7is now "
                                        + (!currentState ? "&eENABLED" : "&cDISABLED")));
                    });
                });
            } else if (event.isRightClick()) {
                openModuleSettings(player, moduleName);
            }
        }
    }

    private void handleSettingsClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null)
            return;

        if (clicked.getType() == Material.ARROW) {
            String title = ChatColor.stripColor(event.getView().getTitle());
            if (title != null && title.contains(" » ")) {
                // back button:
                openMainMenu(player);
            }
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null)
            return;

        String fullPath = "";
        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    if (line != null) {
                        String stripped = ChatColor.stripColor(line);
                        if (stripped != null && stripped.startsWith("Path: ")) {
                            fullPath = stripped.replace("Path: ", "");
                            break;
                        }
                    }
                }
            }
        }
        if (fullPath.isEmpty())
            return;

        final String finalPath = fullPath;
        if (clicked.getType() == Material.CHEST) {
            String name = "Folder";
            String display = meta.getDisplayName();
            String stripped = ChatColor.stripColor(display);
            if (stripped != null) {
                name = stripped.replace("📁 ", "");
            }
            openConfigSection(player, name, finalPath);
            return;
        }

        Object currentVal = plugin.getConfig().get(finalPath);
        if (currentVal instanceof Boolean b) {
            plugin.getConfig().set(finalPath, !b);
        } else if (currentVal instanceof Number n) {
            double modifier = event.isLeftClick() ? 1.0 : -1.0;
            if (n instanceof Integer i) {
                plugin.getConfig().set(finalPath, i + (int) modifier);
            } else {
                plugin.getConfig().set(finalPath, n.doubleValue() + modifier);
            }
        }

        CompletableFuture.runAsync(() -> {
            plugin.saveConfig();
            plugin.getModuleManager().reloadModules();
        }).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Refresh current menu
                String viewTitle = ChatColor.stripColor(event.getView().getTitle());
                String name = "Settings";
                if (viewTitle != null) {
                    name = viewTitle.contains(" » ") ? viewTitle.split(" » ")[1] : viewTitle;
                }
                String parentPath = finalPath.contains(".") ? finalPath.substring(0, finalPath.lastIndexOf("."))
                        : finalPath;
                openConfigSection(player, name, parentPath);

                String settingName = ChatColor.stripColor(meta.getDisplayName());
                player.sendMessage(ColorUtil.translate("&a[SKGuard] &e" + settingName + " &7updated."));
            });
        });
    }

    private void handlePresetsClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;
        
        if (clicked.getType() == Material.ARROW) {
            openMainMenu(player);
            return;
        }
        
        if (clicked.getItemMeta() == null) return;
        String name = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        try {
            com.skguard.util.SecurityPresets.Preset preset = com.skguard.util.SecurityPresets.Preset.valueOf(name);
            com.skguard.util.SecurityPresets.applyPreset(plugin, preset);
            player.sendMessage(ColorUtil.translate("&a[SKGuard] &f" + name + " &ePreset applied successfully!"));
            player.closeInventory();
        } catch (Exception e) {
            // Not a preset item
        }
    }
}

