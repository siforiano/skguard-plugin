package com.skguard;

import org.bukkit.plugin.java.JavaPlugin;

import com.skguard.api.Edition;
import com.skguard.api.ModuleGroup;
import com.skguard.api.ModuleManager;
import com.skguard.api.SecurityModule;
import com.skguard.api.SKGuardCore;
import com.skguard.config.ConfigManager;
import com.skguard.config.LanguageManager;
import com.skguard.database.DatabaseManager;
import com.skguard.integration.DiscordWebhook;
import com.skguard.logger.AuditManager;
import com.skguard.logger.BypassTracker;
import com.skguard.logger.LogManager;
import com.skguard.menu.MenuManager;
import com.skguard.modules.afk.AntiAFK;
import com.skguard.modules.anticheat.CheckManager;
import com.skguard.modules.anticheat.checks.combat.KillauraA;
import com.skguard.modules.anticheat.checks.combat.Reach;
import com.skguard.modules.anticheat.checks.misc.InventoryMove;
import com.skguard.modules.anticheat.checks.movement.Flight;
import com.skguard.modules.anticheat.checks.movement.LiquidWalk;
import com.skguard.modules.anticheat.checks.movement.NoWeb;
import com.skguard.modules.anticheat.checks.movement.SpeedA;
import com.skguard.modules.auth.AuthModule;
import com.skguard.modules.auth.BruteForceProtector;
import com.skguard.modules.auth.FastLoginEngine;
import com.skguard.modules.auth.InventorySnapshotModule;
import com.skguard.modules.auth.PremiumModule;
import com.skguard.modules.auth.TOTPManager;
import com.skguard.modules.auth.UserSecurityModule;
import com.skguard.modules.bot.BehaviorMonitor;
import com.skguard.modules.bot.GhostGuard;
import com.skguard.modules.bot.NameValidator;
import com.skguard.modules.captcha.CaptchaModule;
import com.skguard.modules.chat.ChatFilter;
import com.skguard.modules.chat.PrivacyGuard;
import com.skguard.modules.chat.ScamGuard;
import com.skguard.modules.command.CommandBlocker;
import com.skguard.modules.command.CommandGuard;
import com.skguard.modules.command.GhostCommandModule;
import com.skguard.modules.connection.ConnectionManager;
import com.skguard.modules.connection.WhitelistManager;
import com.skguard.modules.discord.DiscordLinkModule;
import com.skguard.modules.exploit.AbsoluteSecurityInterceptor;
import com.skguard.modules.exploit.AntiExploit;
import com.skguard.modules.exploit.IllegalItemsModule;
import com.skguard.modules.exploit.PacketInspector;
import com.skguard.modules.exploit.PluginGuardModule;
import com.skguard.modules.exploit.ProxySourceGuard;
import com.skguard.modules.geo.GeoIPModule;
import com.skguard.modules.geo.TravelGuard;
import com.skguard.modules.identity.AltLinker;
import com.skguard.modules.identity.IdentityGuard;
import com.skguard.modules.maintenance.MaintenanceModule;
import com.skguard.modules.movement.MovementVerification;
import com.skguard.modules.op.OpGuard;
import com.skguard.modules.panic.PanicModule;
import com.skguard.modules.panic.RollbackManager;
import com.skguard.modules.punish.AutoPunishModule;
import com.skguard.modules.punish.ModerationModule;
import com.skguard.modules.punish.ShadowBanModule;
import com.skguard.modules.ratelimit.RateLimiter;
import com.skguard.modules.report.ReportModule;
import com.skguard.modules.staff.CommandFirewallModule;
import com.skguard.modules.staff.QuarantineManager;
import com.skguard.modules.staff.StaffAnalyticsModule;
import com.skguard.modules.staff.StaffPIN;
import com.skguard.modules.staff.StaffRateLimiter;
import com.skguard.modules.staff.StaffSecurity;
import com.skguard.modules.trade.TradeGuard;
import com.skguard.modules.vpn.VPNDetector;

public class SKGuard extends JavaPlugin implements SKGuardCore {

    private static SKGuard instance;
    private Edition edition;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private com.skguard.database.RedisManager redisManager;
    private com.skguard.database.SessionManager sessionManager;
    private LanguageManager languageManager;
    private LogManager logManager;
    private ModuleManager moduleManager;
    private MenuManager menuManager;
    private com.skguard.database.PunishmentManager punishmentManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize Console Utility (Bukkit)
        com.skguard.util.ConsoleUtil.initialize(null);

        // Detect edition (Lite or Premium)
        this.edition = detectEdition();

        // Banner and Phase 1
        com.skguard.util.ConsoleUtil.sendBanner();
        com.skguard.util.ConsoleUtil.phase("Core Initialization");

        // Initialize Logger
        this.logManager = new LogManager(this);
        this.logManager.logInfo("Booting SKGuard " + edition.getDisplayName() + "...");

        // Load Config
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();
        
        // Security: Master Key Rotation
        checkMasterKey();

        // Initialize Database
        this.databaseManager = new DatabaseManager(this);
        this.sessionManager = new com.skguard.database.SessionManager(this);
        this.databaseManager.initialize();

        this.punishmentManager = new com.skguard.database.PunishmentManager(this);
        this.punishmentManager.initialize();

        // Initialize Redis
        this.redisManager = new com.skguard.database.RedisManager(this);
        this.redisManager.init();

        // Initialize Language Manager
        this.languageManager = new LanguageManager(this);

        // Phase 2: Module Registration
        com.skguard.util.ConsoleUtil.phase("Security Modules");
        this.moduleManager = new ModuleManager(this);
        
        // Critical Logger first
        this.moduleManager.registerModule(ModuleGroup.SYSTEM, new BypassTracker(this));

        // Initialize Menu Manager
        this.menuManager = new MenuManager(this);
        getServer().getPluginManager().registerEvents(this.menuManager, this);

        // Register Modules
        registerModules();

        // Phase 3: Finalization
        com.skguard.util.ConsoleUtil.phase("Command & Task Systems");
        com.skguard.util.ConsoleUtil.info("Attempting to register commands...");

        // REFACTOR: Command Manager Registration (10/10 Architecture)
        com.skguard.commands.framework.CommandManager cmdManager = new com.skguard.commands.framework.CommandManager(
                this);

        // System
        cmdManager.register(new com.skguard.commands.sub.SystemCommand(this, "reload"));
        cmdManager.register(new com.skguard.commands.sub.SystemCommand(this, "audit"));
        cmdManager.register(new com.skguard.commands.sub.SystemCommand(this, "panic"));
        cmdManager.register(new com.skguard.commands.sub.SystemCommand(this, "maintenance"));
        cmdManager.register(new com.skguard.commands.sub.SystemCommand(this, "info"));
        cmdManager.register(new com.skguard.commands.sub.SystemCommand(this, "testwebhook"));

        // GUI
        cmdManager.register(new com.skguard.commands.sub.GuiCommand(this));
        cmdManager.register(new com.skguard.commands.sub.SetupCommand(this));

        // Punish
        com.skguard.commands.punish.BanCommand banCmd = new com.skguard.commands.punish.BanCommand(this);
        cmdManager.register(banCmd);
        cmdManager.register("tempban", banCmd);

        com.skguard.commands.punish.MuteCommand muteCmd = new com.skguard.commands.punish.MuteCommand(this);
        cmdManager.register(muteCmd);
        cmdManager.register("tempmute", muteCmd);

        // Moderation
        cmdManager.register(new com.skguard.commands.sub.ModerationCommand(this, "warn"));
        cmdManager.register(new com.skguard.commands.sub.ModerationCommand(this, "unban"));
        cmdManager.register(new com.skguard.commands.sub.ModerationCommand(this, "unmute"));
        cmdManager.register(new com.skguard.commands.sub.ModerationCommand(this, "history"));
        cmdManager.register(new com.skguard.commands.sub.ModerationCommand(this, "alts"));
        cmdManager.register(new com.skguard.commands.sub.ModerationCommand(this, "lock"));
        cmdManager.register(new com.skguard.commands.sub.ModerationCommand(this, "unlock"));

        // Register default commands
        registerCommand("SKGuard", cmdManager);
        registerCommand("pin", new PINCommand(this));
        registerCommand("report", new ReportCommand(this));

        // Start Global Tasks
        startGlobalTasks();

        com.skguard.util.ConsoleUtil.phase("Protection Active");
        com.skguard.util.ConsoleUtil.success("SKGuard is now shielding your server.");
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        com.skguard.util.ConsoleUtil.info("Registering command: /" + name);
        org.bukkit.command.PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            if (executor instanceof org.bukkit.command.TabCompleter tc) {
                cmd.setTabCompleter(tc);
            }
            com.skguard.util.ConsoleUtil.info("Successfully registered command: /" + name);
        } else {
            com.skguard.util.ConsoleUtil.error("CRITICAL ERROR: Command /" + name + " NOT FOUND in plugin.yml!");
            com.skguard.util.ConsoleUtil
                    .error("Please verify that plugin.yml contains '" + name + "' under 'commands:'.");
        }
    }

    private void checkMasterKey() {
        String key = getConfig().getString("general.master-key");
        if (key == null || key.equals("SKGuard_Enterprise_SuperSecret_2026!") || key.isEmpty()) {
            String newKey = java.util.UUID.randomUUID().toString().replace("-", "") + java.util.UUID.randomUUID().toString().replace("-", "");
            getConfig().set("general.master-key", newKey);
            saveConfig();
            logManager.logWarn("SECURITY: Default master-key detected and rotated for your safety.");
        }
    }

    private void startGlobalTasks() {
        // Optimization: Tasks are now delegated to their respective modules for
        // zero-waste consumption.
    }

    private void registerModules() {
        // --- 1. Staff Security ---
        this.moduleManager.registerModule(ModuleGroup.STAFF, new StaffSecurity(this));
        this.moduleManager.registerModule(ModuleGroup.STAFF, new StaffPIN(this));
        this.moduleManager.registerModule(ModuleGroup.STAFF, new StaffRateLimiter(this));
        this.moduleManager.registerModule(ModuleGroup.STAFF, new StaffAnalyticsModule(this));
        this.moduleManager.registerModule(ModuleGroup.STAFF, new QuarantineManager(this));
        this.moduleManager.registerModule(ModuleGroup.STAFF, new OpGuard(this));

        // --- 2. Identity & Account ---
        this.moduleManager.registerModule(ModuleGroup.IDENTITY, new AuthModule(this));
        this.moduleManager.registerModule(ModuleGroup.IDENTITY, new FastLoginEngine(this));
        this.moduleManager.registerModule(ModuleGroup.IDENTITY, new BruteForceProtector(this));
        this.moduleManager.registerModule(ModuleGroup.IDENTITY, new TOTPManager(this));
        this.moduleManager.registerModule(ModuleGroup.IDENTITY, new PremiumModule(this));
        this.moduleManager.registerModule(ModuleGroup.IDENTITY, new IdentityGuard(this));
        this.moduleManager.registerModule(ModuleGroup.IDENTITY, new AltLinker(this));
        this.moduleManager.registerModule(ModuleGroup.IDENTITY, new UserSecurityModule(this));
        this.moduleManager.registerModule(ModuleGroup.IDENTITY, new InventorySnapshotModule(this));

        // --- 3. Advanced Defense ---
        this.moduleManager.registerModule(ModuleGroup.DEFENSE, new CaptchaModule(this));
        this.moduleManager.registerModule(ModuleGroup.DEFENSE, new GeoIPModule(this));
        this.moduleManager.registerModule(ModuleGroup.DEFENSE, new VPNDetector(this));
        this.moduleManager.registerModule(ModuleGroup.DEFENSE, new TravelGuard(this));
        this.moduleManager.registerModule(ModuleGroup.DEFENSE, new MovementVerification(this));
        this.moduleManager.registerModule(ModuleGroup.DEFENSE, new RateLimiter(this));
        this.moduleManager.registerModule(ModuleGroup.DEFENSE, new AntiAFK(this));

        // --- 4. Anti-Cheat (Neo-Guard) ---
        CheckManager acManager = new CheckManager(this);
        this.moduleManager.registerModule(ModuleGroup.ANTICHEAT, acManager);
        acManager.getChecks().add(new Flight(this));
        acManager.getChecks().add(new SpeedA(this));
        acManager.getChecks().add(new KillauraA(this));
        acManager.getChecks().add(new LiquidWalk(this));
        acManager.getChecks().add(new InventoryMove(this));
        acManager.getChecks().add(new Reach(this));
        acManager.getChecks().add(new NoWeb(this));

        // --- 5. Anti-Exploit ---
        this.moduleManager.registerModule(ModuleGroup.EXPLOIT, new AntiExploit(this));
        this.moduleManager.registerModule(ModuleGroup.EXPLOIT, new AbsoluteSecurityInterceptor(this));
        this.moduleManager.registerModule(ModuleGroup.EXPLOIT, new IllegalItemsModule(this));
        this.moduleManager.registerModule(ModuleGroup.EXPLOIT, new PluginGuardModule(this));
        this.moduleManager.registerModule(ModuleGroup.EXPLOIT, new PacketInspector(this));
        this.moduleManager.registerModule(ModuleGroup.EXPLOIT, new ProxySourceGuard(this));
        this.moduleManager.registerModule(ModuleGroup.EXPLOIT, new CommandBlocker(this));
        this.moduleManager.registerModule(ModuleGroup.EXPLOIT, new CommandGuard(this));
        this.moduleManager.registerModule(ModuleGroup.EXPLOIT, new TradeGuard(this));

        // --- 6. Social & Moderation ---
        this.moduleManager.registerModule(ModuleGroup.SOCIAL, new ChatFilter(this));
        this.moduleManager.registerModule(ModuleGroup.SOCIAL, new ScamGuard(this));
        this.moduleManager.registerModule(ModuleGroup.SOCIAL, new PrivacyGuard(this));
        this.moduleManager.registerModule(ModuleGroup.SOCIAL, new ModerationModule(this));
        this.moduleManager.registerModule(ModuleGroup.SOCIAL, new ReportModule(this));
        this.moduleManager.registerModule(ModuleGroup.SOCIAL, new AutoPunishModule(this));
        this.moduleManager.registerModule(ModuleGroup.SOCIAL, new ShadowBanModule(this));

        // --- 7. System & Utilities ---
        this.moduleManager.registerModule(ModuleGroup.SYSTEM, new ConnectionManager(this));
        this.moduleManager.registerModule(ModuleGroup.SYSTEM, new NameValidator(this));
        this.moduleManager.registerModule(ModuleGroup.SYSTEM, new WhitelistManager(this));
        this.moduleManager.registerModule(ModuleGroup.SYSTEM, new AuditManager(this));
        this.moduleManager.registerModule(ModuleGroup.SYSTEM, new PanicModule(this));
        this.moduleManager.registerModule(ModuleGroup.SYSTEM, new MaintenanceModule(this));
        this.moduleManager.registerModule(ModuleGroup.SYSTEM, new RollbackManager(this));
        this.moduleManager.registerModule(ModuleGroup.SYSTEM, new GhostGuard(this));
        this.moduleManager.registerModule(ModuleGroup.SYSTEM, new GhostCommandModule(this));
        this.moduleManager.registerModule(ModuleGroup.SYSTEM, new CommandFirewallModule(this));
        this.moduleManager.registerModule(ModuleGroup.SYSTEM, new DiscordLinkModule(this));
        this.moduleManager.registerModule(ModuleGroup.SYSTEM, new DiscordWebhook(this));
        this.moduleManager.registerModule(ModuleGroup.SYSTEM, new BehaviorMonitor(this));
        this.moduleManager.registerModule(ModuleGroup.SYSTEM, new com.skguard.modules.system.ConflictManager(this));

        // --- 8. PREMIUM FEATURES (Only in Premium Edition) ---
        if (edition.isPremium()) {
            logManager.logInfo("[Premium] Loading exclusive premium modules...");

            try {
                // Premium Core Modules
                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.skguard.api.SecurityModule) Class.forName("com.skguard.modules.premium.WebPanelModule")
                                .getConstructor(SKGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.skguard.api.SecurityModule) Class
                                .forName("com.skguard.modules.premium.MLDetectionModule")
                                .getConstructor(SKGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.skguard.api.SecurityModule) Class
                                .forName("com.skguard.modules.premium.CloudBlacklistModule")
                                .getConstructor(SKGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.skguard.api.SecurityModule) Class
                                .forName("com.skguard.modules.premium.ForensicsModule")
                                .getConstructor(SKGuard.class).newInstance(this));

                // Premium Anti-Cheat Checks
                acManager.getChecks()
                        .add((com.skguard.modules.anticheat.Check) Class
                                .forName("com.skguard.modules.premium.checks.AutoClickerCheck")
                                .getConstructor(SKGuard.class).newInstance(this));
                acManager.getChecks()
                        .add((com.skguard.modules.anticheat.Check) Class
                                .forName("com.skguard.modules.premium.checks.AimAssistCheck")
                                .getConstructor(SKGuard.class).newInstance(this));
                acManager.getChecks()
                        .add((com.skguard.modules.anticheat.Check) Class
                                .forName("com.skguard.modules.premium.checks.VelocityCheck")
                                .getConstructor(SKGuard.class).newInstance(this));
                acManager.getChecks()
                        .add((com.skguard.modules.anticheat.Check) Class
                                .forName("com.skguard.modules.premium.checks.TimerCheck")
                                .getConstructor(SKGuard.class).newInstance(this));
                acManager.getChecks()
                        .add((com.skguard.modules.anticheat.Check) Class
                                .forName("com.skguard.modules.premium.checks.ScaffoldCheck")
                                .getConstructor(SKGuard.class).newInstance(this));
                acManager.getChecks()
                        .add((com.skguard.modules.anticheat.Check) Class
                                .forName("com.skguard.modules.premium.checks.FastBowCheck")
                                .getConstructor(SKGuard.class).newInstance(this));
                acManager.getChecks()
                        .add((com.skguard.modules.anticheat.Check) Class
                                .forName("com.skguard.modules.premium.checks.CriticalsCheck")
                                .getConstructor(SKGuard.class).newInstance(this));
                acManager.getChecks()
                        .add((com.skguard.modules.anticheat.Check) Class
                                .forName("com.skguard.modules.premium.checks.NoFallCheck")
                                .getConstructor(SKGuard.class).newInstance(this));

                // Additional Premium Security Modules
                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.skguard.api.SecurityModule) Class.forName("com.skguard.modules.premium.BiometryModule")
                                .getConstructor(SKGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.skguard.api.SecurityModule) Class
                                .forName("com.skguard.modules.premium.DiscordBotModule")
                                .getConstructor(SKGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.skguard.api.SecurityModule) Class
                                .forName("com.skguard.modules.premium.EmailNotificationModule")
                                .getConstructor(SKGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.skguard.api.SecurityModule) Class.forName("com.skguard.modules.premium.TicketModule")
                                .getConstructor(SKGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.skguard.api.SecurityModule) Class
                                .forName("com.skguard.modules.premium.BotFirewallModule")
                                .getConstructor(SKGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.skguard.api.SecurityModule) Class
                                .forName("com.skguard.modules.premium.PacketObfuscator")
                                .getConstructor(SKGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.skguard.api.SecurityModule) Class
                                .forName("com.skguard.modules.premium.ForensicsReplayModule")
                                .getConstructor(SKGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.skguard.api.SecurityModule) Class
                                .forName("com.skguard.modules.premium.GlobalTrustModule")
                                .getConstructor(SKGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.skguard.api.SecurityModule) Class
                                .forName("com.skguard.modules.premium.PredictiveBanIA")
                                .getConstructor(SKGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.skguard.api.SecurityModule) Class
                                .forName("com.skguard.modules.premium.ShadowMirrorInstance")
                                .getConstructor(SKGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.skguard.api.SecurityModule) Class.forName("com.skguard.modules.premium.ThreatAnalyzer")
                                .getConstructor(SKGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.skguard.api.SecurityModule) Class.forName("com.skguard.modules.premium.HealthAnalyzer")
                                .getConstructor(SKGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.skguard.api.SecurityModule) Class
                                .forName("com.skguard.modules.premium.WeeklyReportModule")
                                .getConstructor(SKGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.skguard.api.SecurityModule) Class.forName("com.skguard.modules.premium.TraceEngine")
                                .getConstructor(SKGuard.class).newInstance(this));

                logManager.logInfo("[Premium] Loaded 19 premium modules + 8 advanced checks!");

                // Final Horizon Polish: Pro Config Validator
                new com.skguard.config.ConfigValidator(this).validate();
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException
                    | java.lang.reflect.InvocationTargetException e) {
                logManager.logError("[Premium] Failed to load perfection-phase modules: " + e.getMessage());
            }
        } else {
            logManager.logInfo("[Lite] Running in Lite mode - Premium features disabled");
            logManager.logInfo("[Lite] Upgrade to Premium for Web Panel, ML Detection, Cloud Blacklist, and more!");
        }
        com.skguard.util.ConsoleUtil.phase("Module Registration Complete");
    }

    public <T extends SecurityModule> T getModule(Class<T> clazz) {
        return moduleManager.getModule(clazz);
    }

    public StaffPIN getStaffPIN() {
        return getModule(StaffPIN.class);
    }

    public com.skguard.database.SessionManager getSessionManager() {
        return sessionManager;
    }

    public AuditManager getAuditManager() {
        return getModule(AuditManager.class);
    }

    public WhitelistManager getWhitelistManager() {
        return getModule(WhitelistManager.class);
    }

    public DiscordWebhook getDiscordWebhook() {
        return getModule(DiscordWebhook.class);
    }

    public PremiumModule getPremiumModule() {
        return getModule(PremiumModule.class);
    }

    public AuthModule getAuthModule() {
        return getModule(AuthModule.class);
    }

    public CaptchaModule getCaptchaModule() {
        return getModule(CaptchaModule.class);
    }

    public BehaviorMonitor getBehaviorMonitor() {
        return getModule(BehaviorMonitor.class);
    }

    public com.skguard.database.PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public PanicModule getPanicModule() {
        return getModule(PanicModule.class);
    }

    public MaintenanceModule getMaintenanceModule() {
        return getModule(MaintenanceModule.class);
    }

    public PacketInspector getPacketInspector() {
        return getModule(PacketInspector.class);
    }

    public ReportModule getReportModule() {
        return getModule(ReportModule.class);
    }

    public IdentityGuard getIdentityGuard() {
        return getModule(IdentityGuard.class);
    }

    public AutoPunishModule getAutoPunishModule() {
        return getModule(AutoPunishModule.class);
    }

    public DiscordLinkModule getDiscordLink() {
        return getModule(DiscordLinkModule.class);
    }

    public ShadowBanModule getShadowBan() {
        return getModule(ShadowBanModule.class);
    }

    public TravelGuard getTravelGuard() {
        return getModule(TravelGuard.class);
    }

    public CommandGuard getCommandGuard() {
        return getModule(CommandGuard.class);
    }

    public StaffAnalyticsModule getStaffAnalytics() {
        return getModule(StaffAnalyticsModule.class);
    }

    public QuarantineManager getQuarantineManager() {
        return getModule(QuarantineManager.class);
    }

    public ScamGuard getScamGuard() {
        return getModule(ScamGuard.class);
    }

    public StaffRateLimiter getStaffRateLimiter() {
        return getModule(StaffRateLimiter.class);
    }

    public UserSecurityModule getUserSecurity() {
        return getModule(UserSecurityModule.class);
    }

    public PrivacyGuard getPrivacyGuard() {
        return getModule(PrivacyGuard.class);
    }

    public TradeGuard getTradeGuard() {
        return getModule(TradeGuard.class);
    }

    public GhostGuard getGhostGuard() {
        return getModule(GhostGuard.class);
    }

    public AltLinker getAltLinker() {
        return getModule(AltLinker.class);
    }

    public RollbackManager getRollbackManager() {
        return getModule(RollbackManager.class);
    }

    public GhostCommandModule getGhostCommands() {
        return getModule(GhostCommandModule.class);
    }

    public CommandFirewallModule getCommandFirewall() {
        return getModule(CommandFirewallModule.class);
    }

    public InventorySnapshotModule getInventorySnapshots() {
        return getModule(InventorySnapshotModule.class);
    }

    public MenuManager getMenuManager() {
        return menuManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    /**
     * Gets the edition of SKGuard currently running.
     * 
     * @return the edition (Lite or Premium)
     */
    public Edition getEdition() {
        return edition;
    }

    /**
     * Detects which edition of SKGuard is running by reading 
     * the build-time properties file.
     * 
     * @return Edition.PREMIUM if the edition property is Premium, Edition.LITE otherwise
     */
    private Edition detectEdition() {
        try (java.io.InputStream input = getClass().getResourceAsStream("/SKGuard.properties")) {
            if (input == null) {
                // Fallback to class detection if properties file is missing
                try {
                    Class.forName("com.skguard.modules.premium.WebPanelModule");
                    return Edition.PREMIUM;
                } catch (ClassNotFoundException e) {
                    return Edition.LITE;
                }
            }
            
            java.util.Properties prop = new java.util.Properties();
            prop.load(input);
            String edition = prop.getProperty("edition", "Lite");
            return edition.equalsIgnoreCase("Premium") ? Edition.PREMIUM : Edition.LITE;
        } catch (java.io.IOException e) {
            return Edition.LITE;
        }
    }

    @Override
    public void onDisable() {
        if (this.logManager != null) {
            this.logManager.logInfo("Disabling SKGuard...");
            this.logManager.shutdown();
        }
        if (this.databaseManager != null) {
            this.databaseManager.shutdown();
        }
        if (this.redisManager != null) {
            this.redisManager.shutdown();
        }
        instance = null;
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (configManager != null) {
            configManager.loadConfig();
        }
        if (moduleManager != null) {
            moduleManager.reloadModules();
        }
    }

    public static SKGuard getInstance() {
        return instance;
    }

    @Override
    public org.slf4j.Logger getSlf4jLogger() {
        return null;
    }

    @Override
    public java.util.logging.Logger getBukkitLogger() {
        return getLogger();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public org.bukkit.configuration.file.FileConfiguration getConfig() {
        return super.getConfig();
    }

    @Override
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public com.skguard.database.RedisManager getRedisManager() {
        return redisManager;
    }

    @Override
    public LogManager getLogManager() {
        return logManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }
}

