package com.soulguard.modules.auth;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import com.soulguard.config.LanguageManager;
import com.soulguard.modules.staff.StaffPIN;
import com.soulguard.util.ColorUtil;

public class AuthModule implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private File usersFile;
    private FileConfiguration usersConfig;

    private final Set<UUID> authenticatedPlayers;
    private final Set<UUID> pendingCaptcha;
    private final Set<UUID> pending2FA;
    private final Map<UUID, Long> sessions;

    // CACHE: Avoid spamming DB in reminder tasks
    private final Map<UUID, Boolean> registrationCache = new ConcurrentHashMap<>();

    private int sessionMinutes;
    private int maxRegPerIp;
    private int minPassLength;
    private int captchaRiskThreshold = 60;

    // Restrictions
    private boolean blockMove;
    private boolean blockChat;
    private boolean blockCommands;
    private boolean blockInteract;
    private boolean blockDamageReceive;
    private boolean blockDamageGive;
    private boolean blockItemDrop;
    private boolean blockItemPickup;
    private boolean blockInventory;
    private boolean blindnessEffect;

    public AuthModule(SoulGuard plugin) {
        this.plugin = plugin;
        this.authenticatedPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.pendingCaptcha = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.pending2FA = Collections.newSetFromMap(new ConcurrentHashMap<>());
        this.sessions = new ConcurrentHashMap<>();
    }

    @Override
    public String getName() {
        return "AuthModule";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        loadUsers();
        reload();
    }

    @Override
    public void disable() {
        this.enabled = false;
        authenticatedPlayers.clear();
        pendingCaptcha.clear();
        pending2FA.clear();
        sessions.clear();
        registrationCache.clear();
    }

    @Override
    public void reload() {
        FileConfiguration config = plugin.getConfig();
        String path = "modules.AuthModule.";
        this.sessionMinutes = config.getInt(path + "session-minutes", 30);
        this.maxRegPerIp = config.getInt(path + "max-reg-per-ip", 3);
        this.minPassLength = config.getInt(path + "password.min-length", 6);
        this.captchaRiskThreshold = config.getInt("modules.CaptchaModule.risk-threshold", 60);

        // Restrictions (Preserving defaults or mapping to new structure if needed)
        // Note: New config doesn't have restrictions block but we keep it for now
        this.blockMove = config.getBoolean(path + "restrictions.block-move", true);
        this.blockChat = config.getBoolean(path + "restrictions.block-chat", true);
        this.blockCommands = config.getBoolean(path + "restrictions.block-commands", true);
        this.blockInteract = config.getBoolean(path + "restrictions.block-interact", true);
        this.blockDamageReceive = config.getBoolean(path + "restrictions.block-damage-receive", true);
        this.blockDamageGive = config.getBoolean(path + "restrictions.block-damage-give", true);
        this.blockItemDrop = config.getBoolean(path + "restrictions.block-item-drop", true);
        this.blockItemPickup = config.getBoolean(path + "restrictions.block-item-pickup", true);
        this.blockInventory = config.getBoolean(path + "restrictions.block-inventory", true);
        this.blindnessEffect = config.getBoolean(path + "restrictions.blindness-effect", true);
    }

    private void loadUsers() {
        usersFile = new File(plugin.getDataFolder(), "users.yml");
        if (!usersFile.exists()) {
            try {
                usersFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogManager().logError("Could not create users.yml");
            }
        }
        usersConfig = YamlConfiguration.loadConfiguration(usersFile);
    }

    private void saveUsers() {
        if (usersConfig == null || usersFile == null)
            return;

        // Serialization to string is fast and safe on main thread
        String data = usersConfig.saveToString();

        // File I/O is slow and should be async
        CompletableFuture.runAsync(() -> {
            try {
                java.nio.file.Files.writeString(usersFile.toPath(), data, java.nio.charset.StandardCharsets.UTF_8);
            } catch (IOException e) {
                plugin.getLogManager().logError("Could not save users.yml (Async): " + e.getMessage());
            }
        });
    }

    private CompletableFuture<Boolean> isRegistered(UUID uuid) {
        if (registrationCache.containsKey(uuid)) {
            return CompletableFuture.completedFuture(registrationCache.get(uuid));
        }

        return CompletableFuture.supplyAsync(() -> {
            boolean registered = false;
            if (plugin.getDatabaseManager().isMySQL()) {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                        PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM sg_users WHERE uuid = ?")) {
                    ps.setString(1, uuid.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        registered = rs.next();
                    }
                } catch (SQLException e) {
                    plugin.getLogManager().logError("Database error checking registration: " + e.getMessage());
                }
            } else {
                synchronized (this) {
                    registered = usersConfig.contains(uuid.toString() + ".password");
                }
            }
            registrationCache.put(uuid, registered);
            return registered;
        });
    }

    private void registerPlayer(Player player, String password) {
        CompletableFuture.runAsync(() -> {
            String hash = PasswordUtil.hashPassword(password);
            String uuid = player.getUniqueId().toString();
            java.net.InetSocketAddress addr = player.getAddress();
            String ip = addr != null ? addr.getHostString() : "unknown";
            long now = System.currentTimeMillis();

            if (plugin.getDatabaseManager().isMySQL()) {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                        PreparedStatement ps = conn.prepareStatement(
                                "INSERT INTO sg_users (uuid, username, password, ip, reg_date) VALUES (?, ?, ?, ?, ?)")) {
                    ps.setString(1, uuid);
                    ps.setString(2, player.getName());
                    ps.setString(3, hash);
                    ps.setString(4, ip != null ? ip : "unknown");
                    ps.setLong(5, now);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogManager().logError("Database error registering player: " + e.getMessage());
                }
            } else {
                synchronized (this) {
                    usersConfig.set(uuid + ".password", hash);
                    usersConfig.set(uuid + ".username", player.getName());
                    usersConfig.set(uuid + ".ip", ip);
                    usersConfig.set(uuid + ".reg-date", now);
                    saveUsers();
                }
            }
            // Update cache
            registrationCache.put(player.getUniqueId(), true);
        }).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                forceLogin(player);
                player.sendMessage(plugin.getLanguageManager().getMessage("auth.success-register"));
            });
        });
    }

    private void verifyPasswordAsync(Player player, String password) {
        CompletableFuture.supplyAsync(() -> {
            String storedHash = null;
            if (plugin.getDatabaseManager().isMySQL()) {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                        PreparedStatement ps = conn
                                .prepareStatement("SELECT password FROM sg_users WHERE uuid = ?")) {
                    ps.setString(1, player.getUniqueId().toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next())
                            storedHash = rs.getString("password");
                    }
                } catch (SQLException e) {
                    plugin.getLogManager().logError("Database error retrieving password: " + e.getMessage());
                }
            } else {
                synchronized (this) {
                    storedHash = usersConfig.getString(player.getUniqueId() + ".password");
                }
            }
            return storedHash;
        }).thenAccept(storedHash -> {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                java.net.InetSocketAddress addrLocal = player.getAddress();
                if (addrLocal == null)
                    return;
                String ip = addrLocal.getHostString();
                BruteForceProtector bfp = plugin.getModule(BruteForceProtector.class);

                if (storedHash != null && PasswordUtil.checkPassword(password, storedHash)) {
                    if (bfp != null)
                        bfp.recordSuccess(ip);

                    // Check for 2FA
                    String totpSecret = usersConfig.getString(player.getUniqueId() + ".2fa_secret");
                    if (totpSecret != null) {
                        pending2FA.add(player.getUniqueId());
                        player.sendMessage(ColorUtil.translate(
                                "&c&l[Security] &7Please enter your **2FA Code** with &e/2fa verify <code>"));
                    } else {
                        forceLogin(player);
                    }

                    UserSecurityModule userSecurity = plugin.getUserSecurity();
                    java.net.InetSocketAddress addr = player.getAddress();
                    if (userSecurity != null && userSecurity.isEnabled() && addr != null && addr.getAddress() != null) {
                        userSecurity.resetFailedAttempts(addr.getAddress().getHostAddress());
                    }
                } else {
                    if (bfp != null)
                        bfp.recordFailure(ip);
                    player.sendMessage(plugin.getLanguageManager().getMessage("auth.incorrect-password"));
                    UserSecurityModule userSecurity = plugin.getUserSecurity();
                    java.net.InetSocketAddress addr = player.getAddress();
                    if (userSecurity != null && userSecurity.isEnabled() && addr != null
                            && addr.getAddress() != null) {
                        userSecurity.recordFailedAttempt(addr.getAddress().getHostAddress());
                    }
                }
            });
        });
    }

    public boolean isAuthenticated(UUID uuid) {
        return authenticatedPlayers.contains(uuid);
    }

    public void forceLogin(Player player) {
        authenticatedPlayers.add(player.getUniqueId());
        sessions.put(player.getUniqueId(), System.currentTimeMillis() + (sessionMinutes * 60 * 1000L));
        player.sendMessage(plugin.getLanguageManager().getMessage("auth.success-login"));

        if (blindnessEffect) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
        }

        // Step 1: Login Successful -> Proceed to Step 2: Staff PIN
        StaffPIN staffPin = plugin.getStaffPIN();
        if (staffPin != null && staffPin.isEnabled() && player.hasPermission("soulguard.staff")) {
            staffPin.startVerification(player);
        }
    }

    private void startAuthReminder(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || authenticatedPlayers.contains(player.getUniqueId())
                        || pendingCaptcha.contains(player.getUniqueId())) {
                    this.cancel();
                    return;
                }

                isRegistered(player.getUniqueId()).thenAccept(registered -> {
                    // Global Session Check (Network Mode)
                    java.net.InetSocketAddress addr = player.getAddress();
                    String ip = (addr != null && addr.getAddress() != null) 
                            ? addr.getAddress().getHostAddress() : "127.0.0.1";
                    if (plugin.getSessionManager() != null) {
                        plugin.getSessionManager().hasActiveSession(player.getUniqueId(), ip).thenAccept(hasSession -> {
                            String lobbyServer = plugin.getConfig().getString("general.lobby-server", "lobby");
                            boolean isLobby = lobbyServer != null && lobbyServer.equalsIgnoreCase(Bukkit.getServer().getName());
                            boolean forceLobbyMFA = plugin.getConfig().getBoolean("general.force-lobby-mfa", true);

                            if (hasSession && (!isLobby || !forceLobbyMFA)) {
                                authenticatedPlayers.add(player.getUniqueId());
                                forceLogin(player);
                                return;
                            }

                            // Normal local auth flow
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                if (player.isOnline() && !authenticatedPlayers.contains(player.getUniqueId())) {
                                    if (!registered) {
                                        player.sendMessage(plugin.getLanguageManager().getMessage("auth.register-required"));
                                    } else {
                                        player.sendMessage(plugin.getLanguageManager().getMessage("auth.login-required"));
                                    }
                                }
                            });
                        });
                    }
                });
            }
        }.runTaskTimer(plugin, 20L, 100L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        authenticatedPlayers.remove(uuid);
        pendingCaptcha.remove(uuid);
        pending2FA.remove(uuid);
        registrationCache.remove(uuid);
        // We keep the session in the map until it expires or another player joins to
        // allow session restore
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled)
            return;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 1. Premium Check (FASTEST)
        PremiumModule premium = plugin.getPremiumModule();
        if (premium != null && premium.isPremium(uuid)) {
            forceLogin(player);
            return;
        }

        // 2. Session Check
        if (sessions.containsKey(uuid)) {
            Long expiry = sessions.get(uuid);
            String storedIp = usersConfig != null ? usersConfig.getString(uuid + ".ip") : null;
            java.net.InetSocketAddress addr = player.getAddress();
            String playerIp = addr != null ? addr.getHostString() : null;
            if (expiry != null && System.currentTimeMillis() < expiry && playerIp != null
                    && playerIp.equals(storedIp)) {
                authenticatedPlayers.add(uuid);
                player.sendMessage(plugin.getLanguageManager().getMessage("auth.session-restored"));

                // Trigger StaffPIN if applicable (even on session restore for extra safety)
                StaffPIN staffPin = plugin.getStaffPIN();
                if (staffPin != null) {
                    staffPin.startVerification(player);
                }
                return;
            }
        }

        authenticatedPlayers.remove(uuid);
        pendingCaptcha.remove(uuid);

        if (blindnessEffect) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1));
        }

        // 3. Risk-Based Captcha
        int riskScore = plugin.getBehaviorMonitor().getRiskScore(player);
        if (riskScore >= captchaRiskThreshold) {
            plugin.getLogManager()
                    .logWarn("Suspicious player " + player.getName() + " (Risk: " + riskScore
                            + "). Triggering Captcha.");
            pendingCaptcha.add(uuid);
            plugin.getCaptchaModule().startCaptcha(player);
        } else {
            startAuthReminder(player);
        }
    }

    @EventHandler
    public void onCaptchaSuccess(com.soulguard.api.event.CaptchaSuccessEvent event) {
        Player player = event.getPlayer();
        if (pendingCaptcha.remove(player.getUniqueId())) {
            startAuthReminder(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!enabled)
            return;
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (authenticatedPlayers.contains(uuid))
            return;

        if (pendingCaptcha.contains(uuid)) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.translate("&c&l[Security] &7Please complete the captcha first."));
            return;
        }

        String[] args = event.getMessage().split(" ");
        String cmd = args[0].toLowerCase();
        LanguageManager lm = plugin.getLanguageManager();

        if (cmd.equals("/login") || cmd.equals("/l")) {
            event.setCancelled(true);
            isRegistered(player.getUniqueId()).thenAccept(registered -> {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!registered) {
                        player.sendMessage(lm.getMessage("auth.not-registered"));
                        return;
                    }
                    if (args.length < 2) {
                        player.sendMessage(lm.getMessage("auth.usage-login"));
                        return;
                    }

                    java.net.InetSocketAddress addr = player.getAddress();
                    if (addr == null)
                        return;
                    String ip = addr.getHostString();
                    BruteForceProtector bfp = plugin.getModule(BruteForceProtector.class);
                    if (bfp != null && bfp.isLocked(ip)) {
                        player.sendMessage(ColorUtil.translate(
                                "&c&l[Security] &7Your IP is temporarily locked due to too many failed login attempts."));
                        return;
                    }

                    verifyPasswordAsync(player, args[1]);
                });
            });
            return;
        }

        if (cmd.equals("/register") || cmd.equals("/reg")) {
            event.setCancelled(true);
            isRegistered(player.getUniqueId()).thenAccept(registered -> {
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    if (registered) {
                        player.sendMessage(lm.getMessage("auth.already-registered"));
                        return;
                    }
                    if (args.length < 3) {
                        player.sendMessage(lm.getMessage("auth.usage-register"));
                        return;
                    }
                    if (!args[1].equals(args[2])) {
                        player.sendMessage(lm.getMessage("auth.passwords-dont-match"));
                        return;
                    }
                    if (args[1].length() < minPassLength) {
                        player.sendMessage(
                                lm.getMessage("auth.password-too-short", "{min}", String.valueOf(minPassLength)));
                        return;
                    }

                    // Registration limit check
                    java.net.InetSocketAddress addr = player.getAddress();
                    String ip = addr != null ? addr.getHostString() : "unknown";
                    int count = 0;
                    if (usersConfig != null) {
                        for (String key : usersConfig.getKeys(false)) {
                            String storedIp = usersConfig.getString(key + ".ip");
                            if (storedIp != null && ip.equals(storedIp)) {
                                count++;
                            }
                        }
                    }
                    if (count >= maxRegPerIp) {
                        player.sendMessage(lm.getMessage("auth.too-many-registrations"));
                        return;
                    }

                    registerPlayer(player, args[1]);
                });
            });
            return;
        }

        if (cmd.equals("/2fa")) {
            event.setCancelled(true);
            String masterKey = plugin.getConfig().getString("general.master-key", "SoulGuardDefaultKey123!");
            
            if (args.length < 2) {
                player.sendMessage(ColorUtil.translate("&c&l[Security] &7Usage: &e/2fa <setup|verify|backup> [code]"));
                return;
            }

            String sub = args[1].toLowerCase();
            TOTPManager totp = plugin.getModule(TOTPManager.class);
            if (totp == null || !totp.isEnabled()) {
                player.sendMessage(ColorUtil.translate("&c&l[Security] &72FA is currently disabled."));
                return;
            }

            if (sub.equals("setup")) {
                if (!isAuthenticated(uuid)) {
                    player.sendMessage(lm.getMessage("auth.please-auth"));
                    return;
                }
                String secret = totp.generateSecret();
                usersConfig.set(uuid + ".temp_2fa_secret", com.soulguard.util.EncryptionUtil.encrypt(secret, masterKey));
                saveUsers();
                player.sendMessage(ColorUtil.translate("&a&l[2FA Setup] &7Your secret key: &e" + secret));
                player.sendMessage(ColorUtil.translate(
                        "&7Use an app like Google Authenticator and then verify with &e/2fa confirm <code>"));
                return;
            }

            if (sub.equals("confirm")) {
                if (!isAuthenticated(uuid)) {
                    player.sendMessage(lm.getMessage("auth.please-auth"));
                    return;
                }
                String encryptedTemp = usersConfig.getString(uuid + ".temp_2fa_secret");
                if (encryptedTemp == null) {
                    player.sendMessage(ColorUtil.translate("&c&l[Security] &7Please use &e/2fa setup &7first."));
                    return;
                }
                String tempSecret = com.soulguard.util.EncryptionUtil.decrypt(encryptedTemp, masterKey);
                
                if (args.length < 3) {
                    player.sendMessage(ColorUtil.translate("&c&l[Security] &7Usage: &e/2fa confirm <code>"));
                    return;
                }
                try {
                    int code = Integer.parseInt(args[2]);
                    if (tempSecret != null && totp.verify(tempSecret, code)) {
                        usersConfig.set(uuid + ".2fa_secret", encryptedTemp);
                        usersConfig.set(uuid + ".temp_2fa_secret", null);
                        
                        // Generate Backup Codes
                        java.util.List<String> backupCodes = totp.generateBackupCodes(10);
                        java.util.List<String> hashedCodes = new java.util.ArrayList<>();
                        for (String bcode : backupCodes) {
                            hashedCodes.add(PasswordUtil.hashPassword(bcode));
                        }
                        usersConfig.set(uuid + ".2fa_backup_codes", hashedCodes);
                        
                        saveUsers();
                        player.sendMessage(ColorUtil.translate("&a&l[Security] &72FA has been successfully enabled!"));
                        player.sendMessage(ColorUtil.translate("&e&l[IMPORTANT] &7Your Backup Codes (SAVE THEM!):"));
                        for (String bcode : backupCodes) {
                            player.sendMessage(ColorUtil.translate("&7- &f" + bcode));
                        }
                    } else {
                        player.sendMessage(ColorUtil.translate("&c&l[Security] &7Invalid 2FA code."));
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.translate("&c&l[Security] &7Code must be a number."));
                }
                return;
            }

            if (sub.equals("verify")) {
                if (!pending2FA.contains(uuid)) {
                    player.sendMessage(ColorUtil.translate("&c&l[Security] &7You don't need to verify 2FA right now."));
                    return;
                }
                String encryptedSecret = usersConfig.getString(uuid + ".2fa_secret");
                if (encryptedSecret == null) {
                    pending2FA.remove(uuid);
                    forceLogin(player);
                    return;
                }
                String secret = com.soulguard.util.EncryptionUtil.decrypt(encryptedSecret, masterKey);

                if (args.length < 3) {
                    player.sendMessage(ColorUtil.translate("&c&l[Security] &7Usage: &e/2fa verify <code|backup_code>"));
                    return;
                }
                
                String input = args[2];
                boolean success = false;
                
                // Try TOTP Code
                try {
                    int code = Integer.parseInt(input);
                    if (secret != null && totp.verify(secret, code)) {
                        success = true;
                    }
                } catch (NumberFormatException ignored) {}

                // Try Backup Code if TOTP failed
                if (!success) {
                    java.util.List<String> hashedCodes = usersConfig.getStringList(uuid + ".2fa_backup_codes");
                    for (int i = 0; i < hashedCodes.size(); i++) {
                        if (PasswordUtil.checkPassword(input, hashedCodes.get(i))) {
                            hashedCodes.remove(i);
                            usersConfig.set(uuid + ".2fa_backup_codes", hashedCodes);
                            saveUsers();
                            success = true;
                            player.sendMessage(ColorUtil.translate("&a&l[Security] &7Backup code used. &e" + hashedCodes.size() + " &7remaining."));
                            break;
                        }
                    }
                }

                if (success) {
                    pending2FA.remove(uuid);
                    forceLogin(player);
                } else {
                    player.sendMessage(ColorUtil.translate("&c&l[Security] &7Invalid code or backup code."));
                }
                return;
            }
        }

        if (cmd.equals("/changepassword") || cmd.equals("/changepass")) {
            event.setCancelled(true);
            if (!authenticatedPlayers.contains(uuid)) {
                player.sendMessage(lm.getMessage("auth.please-auth"));
                return;
            }
            if (args.length < 2) {
                player.sendMessage(ColorUtil.translate("&cUsage: /changepassword <new_password>"));
                return;
            }
            if (args[1].length() < minPassLength) {
                player.sendMessage(lm.getMessage("auth.password-too-short", "{min}", String.valueOf(minPassLength)));
                return;
            }
            
            // Update password logic (Reuse registerPlayer's async logic structure)
            CompletableFuture.runAsync(() -> {
                String hash = PasswordUtil.hashPassword(args[1]);
                if (plugin.getDatabaseManager().isMySQL()) {
                    try (Connection conn = plugin.getDatabaseManager().getConnection();
                         PreparedStatement ps = conn.prepareStatement("UPDATE sg_users SET password = ? WHERE uuid = ?")) {
                        ps.setString(1, hash);
                        ps.setString(2, uuid.toString());
                        ps.executeUpdate();
                    } catch (SQLException e) {
                        plugin.getLogManager().logError("DB error changing password: " + e.getMessage());
                    }
                } else {
                    synchronized (this) {
                        usersConfig.set(uuid.toString() + ".password", hash);
                        saveUsers();
                    }
                }
            }).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(lm.getMessage("auth.change-password-success"));
                });
            });
            return;
        }

        if (blockCommands) {
            event.setCancelled(true);
            player.sendMessage(lm.getMessage("auth.please-auth"));
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (enabled && blockChat && !authenticatedPlayers.contains(uuid)) {
            event.setCancelled(true);
            if (pendingCaptcha.contains(uuid)) {
                event.getPlayer()
                        .sendMessage(ColorUtil.translate("&c&l[Security] &7Please complete the captcha first."));
            } else {
                event.getPlayer().sendMessage(plugin.getLanguageManager().getMessage("auth.please-auth"));
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (enabled && blockMove && !authenticatedPlayers.contains(uuid)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (from.getX() != to.getX() || from.getZ() != to.getZ() || from.getY() != to.getY())) {
                event.setTo(from);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (enabled && blockInteract && !authenticatedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (enabled && blockItemDrop && !authenticatedPlayers.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (enabled && blockItemPickup && event.getEntity() instanceof Player
                && !authenticatedPlayers.contains(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventory(InventoryClickEvent event) {
        if (enabled && blockInventory && event.getWhoClicked() instanceof Player
                && !authenticatedPlayers.contains(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageReceive(EntityDamageEvent event) {
        if (enabled && blockDamageReceive && event.getEntity() instanceof Player
                && !authenticatedPlayers.contains(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageGive(EntityDamageByEntityEvent event) {
        if (enabled && blockDamageGive && event.getDamager() instanceof Player
                && !authenticatedPlayers.contains(event.getDamager().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
