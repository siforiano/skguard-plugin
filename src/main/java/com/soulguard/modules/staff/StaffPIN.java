package com.soulguard.modules.staff;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import com.soulguard.config.LanguageManager;
import com.soulguard.database.PunishmentManager;
import com.soulguard.modules.auth.AuthModule;
import com.soulguard.modules.discord.DiscordLinkModule;
import com.soulguard.util.ColorUtil;

/**
 * Enhanced Staff PIN module with support for both text-based commands
 * and visual pattern-based GUIs (optimal for Bedrock/Mobile).
 */
public class StaffPIN implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private File pinsFile;
    private FileConfiguration pinsConfig;

    private final Set<UUID> pendingPin;
    private final Map<UUID, Integer> attempts;
    private final Map<UUID, BukkitTask> reminderTasks;
    private final Map<UUID, List<Integer>> currentPattern;

    private int maxAttempts;
    private String permission;
    private final String PATTERN_GUI_TITLE = ColorUtil.translate("&8&lStaff &7» &6&lPattern PIN");

    public StaffPIN(SoulGuard plugin) {
        this.plugin = plugin;
        this.pendingPin = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.attempts = new ConcurrentHashMap<>();
        this.reminderTasks = new ConcurrentHashMap<>();
        this.currentPattern = new ConcurrentHashMap<>();
    }

    @Override
    public String getName() {
        return "StaffPIN";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        loadPins();
        reload();
    }

    @Override
    public void disable() {
        this.enabled = false;
        pendingPin.clear();
        attempts.clear();
        currentPattern.clear();
        reminderTasks.values().forEach(BukkitTask::cancel);
        reminderTasks.clear();
    }

    @Override
    public void reload() {
        this.maxAttempts = plugin.getConfig().getInt("modules.StaffPIN.max-attempts", 3);
        this.permission = plugin.getConfig().getString("modules.StaffPIN.permission", "soulguard.staff");
        loadPins();
    }

    private void loadPins() {
        pinsFile = new File(plugin.getDataFolder(), "pins.yml");
        if (!pinsFile.exists()) {
            try {
                pinsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogManager().logError("Could not create pins.yml");
            }
        }
        pinsConfig = YamlConfiguration.loadConfiguration(pinsFile);
    }

    private void savePins() {
        if (pinsConfig == null || pinsFile == null)
            return;

        // Serialization to string is fast and safe on main thread
        String data = pinsConfig.saveToString();

        // File I/O is slow and should be async
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                java.nio.file.Files.writeString(pinsFile.toPath(), data, java.nio.charset.StandardCharsets.UTF_8);
            } catch (IOException e) {
                plugin.getLogManager().logError("Could not save pins.yml (Async): " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Step 2 (Staff PIN) is now triggered exclusively by Step 1 (AuthModule)
        // to ensure linear authentication flow (Login -> PIN -> 2FA).
    }

    public void startVerification(Player player) {
        if (!enabled || !player.hasPermission(permission))
            return;

        AuthModule auth = plugin.getAuthModule();
        if (auth != null && auth.isEnabled() && !auth.isAuthenticated(player.getUniqueId())) {
            return;
        }

        LanguageManager lm = plugin.getLanguageManager();
        plugin.getQuarantineManager().quarantinePlayer(player);
        pendingPin.add(player.getUniqueId());

        hasPinSetAsync(player).thenAccept(hasPin -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (hasPin) {
                    // Check if pattern pin is enabled and preferred for this platform
                    if (plugin.getConfig().getBoolean("modules.StaffSecurity.pattern-pin", true)) {
                        openPatternGUI(player);
                    } else {
                        player.sendMessage(lm.getMessage("staff.pin-required"));
                        startReminder(player);
                    }
                } else {
                    player.sendMessage(lm.getMessage("staff.pin-usage"));
                }
            });
        });
    }

    public void openPatternGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, PATTERN_GUI_TITLE);
        int[] patternSlots = { 20, 21, 22, 29, 30, 31, 38, 39, 40 };

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++)
            inv.setItem(i, filler);

        for (int i = 0; i < patternSlots.length; i++) {
            inv.setItem(patternSlots[i], createItem(Material.IRON_BLOCK, "&fButton #" + (i + 1)));
        }

        inv.setItem(48, createItem(Material.RED_DYE, "&cClear Pattern"));
        inv.setItem(50, createItem(Material.LIME_DYE, "&aConfirm Pattern"));

        currentPattern.put(player.getUniqueId(), new ArrayList<>());
        player.openInventory(inv);
        player.sendMessage(ColorUtil.translate("&a[Staff] &7Please draw your **Visual Pattern**."));
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.translate(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!enabled || !event.getView().getTitle().equals(PATTERN_GUI_TITLE))
            return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player))
            return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        List<Integer> pattern = currentPattern.get(player.getUniqueId());
        if (pattern == null)
            return;

        Material type = clicked.getType();
        switch (type) {
            case IRON_BLOCK -> {
                pattern.add(event.getSlot());
                player.sendMessage(ColorUtil.translate("&7Key recorded &8(&bx" + pattern.size() + "&8)"));
                clicked.setType(Material.GOLD_BLOCK);
            }
            case RED_DYE -> openPatternGUI(player);
            case LIME_DYE -> {
                if (!pattern.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (int slot : pattern)
                        sb.append(slot).append(",");

                    getStoredPinAsync(player.getUniqueId()).thenAccept(storedPin -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            processPinVerification(player, sb.toString(), storedPin, plugin.getLanguageManager());
                        });
                    });
                }
            }
            default -> {
            }
        }
    }

    private java.util.concurrent.CompletableFuture<String> getStoredPinAsync(UUID uuid) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            if (plugin.getDatabaseManager().isMySQL()) {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                        PreparedStatement ps = conn.prepareStatement("SELECT pin FROM sg_pins WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next())
                            return rs.getString("pin");
                    }
                } catch (SQLException ignored) {
                }
            }
            return pinsConfig.getString(uuid.toString());
        });
    }

    private java.util.concurrent.CompletableFuture<Boolean> hasPinSetAsync(Player player) {
        String uuidStr = player.getUniqueId().toString();
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            if (plugin.getDatabaseManager().isMySQL()) {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                        PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM sg_pins WHERE uuid = ?")) {
                    ps.setString(1, uuidStr);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() && rs.getInt(1) > 0;
                    }
                } catch (SQLException e) {
                    plugin.getLogManager().logError("DB check error: " + e.getMessage());
                }
            }
            return pinsConfig != null && pinsConfig.contains(uuidStr);
        });
    }

    public boolean handlePinCommand(Player player, String[] args) {
        if (!enabled)
            return false;
        LanguageManager lm = plugin.getLanguageManager();

        if (args.length == 0) {
            player.sendMessage(lm.getMessage("staff.pin-usage"));
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) {
                player.sendMessage(lm.getMessage("staff.pin-usage"));
                return true;
            }
            // Allow re-setting if admin or not set
            hasPinSetAsync(player).thenAccept(hasPin -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (hasPin && !player.hasPermission("soulguard.admin")) {
                        player.sendMessage(lm.getMessage("staff.pin-already-set"));
                        return;
                    }
                    String hashedPin = com.soulguard.modules.auth.PasswordUtil.hashPassword(args[1]);
                    savePinAsync(player, hashedPin, false);
                });
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("change")) {
            if (args.length < 2) {
                player.sendMessage(ColorUtil.translate("&cUsage: /pin change <new_pin>"));
                return true;
            }
            if (pendingPin.contains(player.getUniqueId())) {
                player.sendMessage(lm.getMessage("staff.pin-required"));
                return true;
            }
            String hashedPin = com.soulguard.modules.auth.PasswordUtil.hashPassword(args[1]);
            savePinAsync(player, hashedPin, true);
            return true;
        }

        final String providedPin = args[0].trim();
        getStoredPinAsync(player.getUniqueId()).thenAccept(storedPin -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                processPinVerification(player, providedPin, storedPin, lm);
            });
        });
        return true;
    }

    private void savePinAsync(Player player, String hashedPin, boolean isChange) {
        if (plugin.getDatabaseManager().isMySQL()) {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                        PreparedStatement ps = conn
                                .prepareStatement("REPLACE INTO sg_pins (uuid, pin) VALUES (?, ?)")) {
                    ps.setString(1, player.getUniqueId().toString());
                    ps.setString(2, hashedPin);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogManager().logError("Database error setting PIN: " + e.getMessage());
                }
            }).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(plugin.getLanguageManager().getMessage(isChange ? "staff.change-pin-success" : "staff.pin-set-success"));
                    pendingPin.remove(player.getUniqueId());
                });
            });
        } else {
            pinsConfig.set(player.getUniqueId().toString(), hashedPin);
            savePins();
            player.sendMessage(plugin.getLanguageManager().getMessage(isChange ? "staff.change-pin-success" : "staff.pin-set-success"));
            pendingPin.remove(player.getUniqueId());
        }
    }

    private void processPinVerification(Player player, String providedPin, String storedPinHashed, LanguageManager lm) {
        if (storedPinHashed == null) {
            player.sendMessage(lm.getMessage("staff.pin-usage"));
            return;
        }

        // IMPROVEMENT: Handle both hashed PINs and pattern strings
        // If it's a pattern GUI, we compare it against the stored pattern
        boolean isPattern = false;
        if (currentPattern.containsKey(player.getUniqueId())) {
            isPattern = true;
        }

        boolean success;
        if (storedPinHashed.startsWith("$argon2id$") || storedPinHashed.startsWith("PBKDF2:")) {
            // Text PIN check
            success = com.soulguard.modules.auth.PasswordUtil.checkPassword(providedPin, storedPinHashed);
        } else {
            // Pattern string check (legacy or special)
            success = providedPin.equals(storedPinHashed);
        }

        if (success) {
            player.sendMessage(lm.getMessage("staff.pin-correct"));
            pendingPin.remove(player.getUniqueId());
            attempts.remove(player.getUniqueId());
            currentPattern.remove(player.getUniqueId());

            BukkitTask task = reminderTasks.remove(player.getUniqueId());
            if (task != null)
                task.cancel();

            // Step 2: PIN Successful -> Proceed to Step 3: Discord 2FA
            DiscordLinkModule discord = plugin.getDiscordLink();
            if (discord != null && discord.isEnabled() && player.hasPermission("soulguard.staff")) {
                discord.triggerVerification(player);
            } else {
                plugin.getQuarantineManager().releasePlayer(player);
                // Create Global Session (Network Mode)
                java.net.InetSocketAddress addr = player.getAddress();
                if (addr != null && addr.getAddress() != null && plugin.getSessionManager() != null) {
                    plugin.getSessionManager().createSession(player.getUniqueId(), addr.getAddress().getHostAddress(), 3600000); // 1 hour session
                }
            }
            player.closeInventory();
        } else {
            int currentAttempts = attempts.getOrDefault(player.getUniqueId(), 0) + 1;
            attempts.put(player.getUniqueId(), currentAttempts);

            if (currentAttempts >= maxAttempts) {
                String ip = "unknown";
                if (player.getAddress() != null && player.getAddress().getAddress() != null) {
                    ip = player.getAddress().getAddress().getHostAddress();
                }
                player.kickPlayer(lm.getMessage("staff.pin-incorrect")
                        .replace("{current}", String.valueOf(currentAttempts))
                        .replace("{max}", String.valueOf(maxAttempts)));
                
                // CRITICAL: Force IP-Ban for 30 minutes to prevent brute-force
                if (!ip.equals("unknown")) {
                    PunishmentManager.Punishment p = new PunishmentManager.Punishment(
                            player.getUniqueId(),
                            ip,
                            PunishmentManager.PunishmentType.BAN,
                            "[SoulGuard] Brute-force attempt detected (Failed PIN)",
                            "SOULGUARD-SYSTEM",
                            System.currentTimeMillis(),
                            System.currentTimeMillis() + (30 * 60 * 1000), // 30 minutes
                            true
                    );
                    plugin.getPunishmentManager().addPunishment(p);
                    plugin.getLogManager().logWarn("IP-Banned " + ip + " for 30m due to Staff PIN brute-force attempt.");
                }
            } else {
                player.sendMessage(lm.getMessage("staff.pin-incorrect")
                        .replace("{current}", String.valueOf(currentAttempts))
                        .replace("{max}", String.valueOf(maxAttempts)));
                if (isPattern) {
                    openPatternGUI(player); // Reset for retry
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        pendingPin.remove(uuid);
        attempts.remove(uuid);
        currentPattern.remove(uuid);
        BukkitTask task = reminderTasks.remove(uuid);
        if (task != null)
            task.cancel();
    }

    private void startReminder(Player player) {
        if (reminderTasks.containsKey(player.getUniqueId()))
            return;

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !pendingPin.contains(player.getUniqueId())) {
                    reminderTasks.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                player.sendMessage(plugin.getLanguageManager().getMessage("staff.pin-required"));
            }
        }.runTaskTimer(plugin, 100L, 100L);
        reminderTasks.put(player.getUniqueId(), task);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent event) {
        if (enabled && pendingPin.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (enabled && pendingPin.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getLanguageManager().getMessage("staff.pin-required"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!enabled || !pendingPin.contains(event.getPlayer().getUniqueId()))
            return;
        String message = event.getMessage().toLowerCase();
        if (!message.startsWith("/pin")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getLanguageManager().getMessage("staff.pin-required"));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!enabled || !event.getView().getTitle().equals(PATTERN_GUI_TITLE))
            return;
        Player player = (Player) event.getPlayer();
        if (pendingPin.contains(player.getUniqueId())) {
            // Re-open if they haven't finished, unless they want to switch to text
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && pendingPin.contains(player.getUniqueId())) {
                        openPatternGUI(player);
                    }
                }
            }.runTaskLater(plugin, 5L);
        }
    }

    public boolean isPending(UUID uuid) {
        return pendingPin.contains(uuid);
    }
}
