package com.soulguard;

import org.bukkit.plugin.java.JavaPlugin;

import com.soulguard.api.Edition;
import com.soulguard.api.ModuleGroup;
import com.soulguard.api.ModuleManager;
import com.soulguard.api.SecurityModule;
import com.soulguard.config.ConfigManager;
import com.soulguard.config.LanguageManager;
import com.soulguard.database.DatabaseManager;
import com.soulguard.integration.DiscordWebhook;
import com.soulguard.logger.AuditManager;
import com.soulguard.logger.BypassTracker;
import com.soulguard.logger.LogManager;
import com.soulguard.menu.MenuManager;
import com.soulguard.modules.afk.AntiAFK;
import com.soulguard.modules.anticheat.CheckManager;
import com.soulguard.modules.anticheat.checks.combat.KillauraA;
import com.soulguard.modules.anticheat.checks.combat.Reach;
import com.soulguard.modules.anticheat.checks.misc.InventoryMove;
import com.soulguard.modules.anticheat.checks.movement.Flight;
import com.soulguard.modules.anticheat.checks.movement.LiquidWalk;
import com.soulguard.modules.anticheat.checks.movement.NoWeb;
import com.soulguard.modules.anticheat.checks.movement.SpeedA;
import com.soulguard.modules.auth.AuthModule;
import com.soulguard.modules.auth.BruteForceProtector;
import com.soulguard.modules.auth.FastLoginEngine;
import com.soulguard.modules.auth.InventorySnapshotModule;
import com.soulguard.modules.auth.PremiumModule;
import com.soulguard.modules.auth.TOTPManager;
import com.soulguard.modules.auth.UserSecurityModule;
import com.soulguard.modules.bot.BehaviorMonitor;
import com.soulguard.modules.bot.GhostGuard;
import com.soulguard.modules.bot.NameValidator;
import com.soulguard.modules.captcha.CaptchaModule;
import com.soulguard.modules.chat.ChatFilter;
import com.soulguard.modules.chat.PrivacyGuard;
import com.soulguard.modules.chat.ScamGuard;
import com.soulguard.modules.command.CommandBlocker;
import com.soulguard.modules.command.CommandGuard;
import com.soulguard.modules.command.GhostCommandModule;
import com.soulguard.modules.connection.ConnectionManager;
import com.soulguard.modules.connection.WhitelistManager;
import com.soulguard.modules.discord.DiscordLinkModule;
import com.soulguard.modules.exploit.AbsoluteSecurityInterceptor;
import com.soulguard.modules.exploit.AntiExploit;
import com.soulguard.modules.exploit.IllegalItemsModule;
import com.soulguard.modules.exploit.PacketInspector;
import com.soulguard.modules.exploit.PluginGuardModule;
import com.soulguard.modules.exploit.ProxySourceGuard;
import com.soulguard.modules.geo.GeoIPModule;
import com.soulguard.modules.geo.TravelGuard;
import com.soulguard.modules.identity.AltLinker;
import com.soulguard.modules.identity.IdentityGuard;
import com.soulguard.modules.maintenance.MaintenanceModule;
import com.soulguard.modules.movement.MovementVerification;
import com.soulguard.modules.op.OpGuard;
import com.soulguard.modules.panic.PanicModule;
import com.soulguard.modules.panic.RollbackManager;
import com.soulguard.modules.punish.AutoPunishModule;
import com.soulguard.modules.punish.ModerationModule;
import com.soulguard.modules.punish.ShadowBanModule;
import com.soulguard.modules.ratelimit.RateLimiter;
import com.soulguard.modules.report.ReportModule;
import com.soulguard.modules.staff.CommandFirewallModule;
import com.soulguard.modules.staff.QuarantineManager;
import com.soulguard.modules.staff.StaffAnalyticsModule;
import com.soulguard.modules.staff.StaffPIN;
import com.soulguard.modules.staff.StaffRateLimiter;
import com.soulguard.modules.staff.StaffSecurity;
import com.soulguard.modules.trade.TradeGuard;
import com.soulguard.modules.vpn.VPNDetector;

public class SoulGuard extends JavaPlugin {

    private static SoulGuard instance;
    private Edition edition;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private com.soulguard.database.RedisManager redisManager;
    private com.soulguard.database.SessionManager sessionManager;
    private LanguageManager languageManager;
    private LogManager logManager;
    private ModuleManager moduleManager;
    private MenuManager menuManager;
    private com.soulguard.database.PunishmentManager punishmentManager;

    @Override
    public void onEnable() {
        instance = this;

        // Detect edition (Lite or Premium)
        this.edition = detectEdition();

        // Banner and Phase 1
        com.soulguard.util.ConsoleUtil.sendBanner();
        com.soulguard.util.ConsoleUtil.phase("Core Initialization");

        // Initialize Logger
        this.logManager = new LogManager(this);
        this.logManager.logInfo("Booting SoulGuard " + edition.getDisplayName() + "...");

        // Load Config
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();

        // Initialize Database
        this.databaseManager = new DatabaseManager(this);
        this.sessionManager = new com.soulguard.database.SessionManager(this);
        this.databaseManager.init();

        this.punishmentManager = new com.soulguard.database.PunishmentManager(this);
        this.punishmentManager.init();

        // Initialize Redis
        this.redisManager = new com.soulguard.database.RedisManager(this);
        this.redisManager.init();

        // Initialize Language Manager
        this.languageManager = new LanguageManager(this);

        // Phase 2: Module Registration
        com.soulguard.util.ConsoleUtil.phase("Security Modules");
        this.moduleManager = new ModuleManager(this);
        
        // Critical Logger first
        this.moduleManager.registerModule(ModuleGroup.SYSTEM, new BypassTracker(this));

        // Initialize Menu Manager
        this.menuManager = new MenuManager(this);
        getServer().getPluginManager().registerEvents(this.menuManager, this);

        // Register Modules
        registerModules();

        // Phase 3: Finalization
        com.soulguard.util.ConsoleUtil.phase("Command & Task Systems");
        com.soulguard.util.ConsoleUtil.info("Attempting to register commands...");

        // REFACTOR: Command Manager Registration (10/10 Architecture)
        com.soulguard.commands.framework.CommandManager cmdManager = new com.soulguard.commands.framework.CommandManager(
                this);

        // System
        cmdManager.register(new com.soulguard.commands.sub.SystemCommand(this, "reload"));
        cmdManager.register(new com.soulguard.commands.sub.SystemCommand(this, "audit"));
        cmdManager.register(new com.soulguard.commands.sub.SystemCommand(this, "panic"));
        cmdManager.register(new com.soulguard.commands.sub.SystemCommand(this, "maintenance"));
        cmdManager.register(new com.soulguard.commands.sub.SystemCommand(this, "info"));
        cmdManager.register(new com.soulguard.commands.sub.SystemCommand(this, "testwebhook"));

        // GUI
        cmdManager.register(new com.soulguard.commands.sub.GuiCommand(this));

        // Punish
        com.soulguard.commands.punish.BanCommand banCmd = new com.soulguard.commands.punish.BanCommand(this);
        cmdManager.register(banCmd);
        cmdManager.register("tempban", banCmd);

        com.soulguard.commands.punish.MuteCommand muteCmd = new com.soulguard.commands.punish.MuteCommand(this);
        cmdManager.register(muteCmd);
        cmdManager.register("tempmute", muteCmd);

        // Moderation
        cmdManager.register(new com.soulguard.commands.sub.ModerationCommand(this, "warn"));
        cmdManager.register(new com.soulguard.commands.sub.ModerationCommand(this, "unban"));
        cmdManager.register(new com.soulguard.commands.sub.ModerationCommand(this, "unmute"));
        cmdManager.register(new com.soulguard.commands.sub.ModerationCommand(this, "history"));
        cmdManager.register(new com.soulguard.commands.sub.ModerationCommand(this, "alts"));
        cmdManager.register(new com.soulguard.commands.sub.ModerationCommand(this, "lock"));
        cmdManager.register(new com.soulguard.commands.sub.ModerationCommand(this, "unlock"));

        // Register default commands
        registerCommand("soulguard", cmdManager);
        registerCommand("pin", new PINCommand(this));
        registerCommand("report", new ReportCommand(this));

        // Start Global Tasks
        startGlobalTasks();

        com.soulguard.util.ConsoleUtil.phase("Protection Active");
        com.soulguard.util.ConsoleUtil.success("SoulGuard is now shielding your server.");
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        com.soulguard.util.ConsoleUtil.info("Registering command: /" + name);
        org.bukkit.command.PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(executor);
            if (executor instanceof org.bukkit.command.TabCompleter tc) {
                cmd.setTabCompleter(tc);
            }
            com.soulguard.util.ConsoleUtil.info("Successfully registered command: /" + name);
        } else {
            com.soulguard.util.ConsoleUtil.error("CRITICAL ERROR: Command /" + name + " NOT FOUND in plugin.yml!");
            com.soulguard.util.ConsoleUtil
                    .error("Please verify that plugin.yml contains '" + name + "' under 'commands:'.");
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
        this.moduleManager.registerModule(ModuleGroup.SYSTEM, new com.soulguard.modules.system.ConflictManager(this));

        // --- 8. PREMIUM FEATURES (Only in Premium Edition) ---
        if (edition.isPremium()) {
            logManager.logInfo("[Premium] Loading exclusive premium modules...");

            try {
                // Premium Core Modules
                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.soulguard.api.SecurityModule) Class.forName("com.soulguard.modules.premium.WebPanelModule")
                                .getConstructor(SoulGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.soulguard.api.SecurityModule) Class
                                .forName("com.soulguard.modules.premium.MLDetectionModule")
                                .getConstructor(SoulGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.soulguard.api.SecurityModule) Class
                                .forName("com.soulguard.modules.premium.CloudBlacklistModule")
                                .getConstructor(SoulGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.soulguard.api.SecurityModule) Class
                                .forName("com.soulguard.modules.premium.ForensicsModule")
                                .getConstructor(SoulGuard.class).newInstance(this));

                // Premium Anti-Cheat Checks
                acManager.getChecks()
                        .add((com.soulguard.modules.anticheat.Check) Class
                                .forName("com.soulguard.modules.premium.checks.AutoClickerCheck")
                                .getConstructor(SoulGuard.class).newInstance(this));
                acManager.getChecks()
                        .add((com.soulguard.modules.anticheat.Check) Class
                                .forName("com.soulguard.modules.premium.checks.AimAssistCheck")
                                .getConstructor(SoulGuard.class).newInstance(this));
                acManager.getChecks()
                        .add((com.soulguard.modules.anticheat.Check) Class
                                .forName("com.soulguard.modules.premium.checks.VelocityCheck")
                                .getConstructor(SoulGuard.class).newInstance(this));
                acManager.getChecks()
                        .add((com.soulguard.modules.anticheat.Check) Class
                                .forName("com.soulguard.modules.premium.checks.TimerCheck")
                                .getConstructor(SoulGuard.class).newInstance(this));
                acManager.getChecks()
                        .add((com.soulguard.modules.anticheat.Check) Class
                                .forName("com.soulguard.modules.premium.checks.ScaffoldCheck")
                                .getConstructor(SoulGuard.class).newInstance(this));
                acManager.getChecks()
                        .add((com.soulguard.modules.anticheat.Check) Class
                                .forName("com.soulguard.modules.premium.checks.FastBowCheck")
                                .getConstructor(SoulGuard.class).newInstance(this));
                acManager.getChecks()
                        .add((com.soulguard.modules.anticheat.Check) Class
                                .forName("com.soulguard.modules.premium.checks.CriticalsCheck")
                                .getConstructor(SoulGuard.class).newInstance(this));
                acManager.getChecks()
                        .add((com.soulguard.modules.anticheat.Check) Class
                                .forName("com.soulguard.modules.premium.checks.NoFallCheck")
                                .getConstructor(SoulGuard.class).newInstance(this));

                // Additional Premium Security Modules
                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.soulguard.api.SecurityModule) Class.forName("com.soulguard.modules.premium.BiometryModule")
                                .getConstructor(SoulGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.soulguard.api.SecurityModule) Class
                                .forName("com.soulguard.modules.premium.DiscordBotModule")
                                .getConstructor(SoulGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.soulguard.api.SecurityModule) Class
                                .forName("com.soulguard.modules.premium.EmailNotificationModule")
                                .getConstructor(SoulGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.soulguard.api.SecurityModule) Class.forName("com.soulguard.modules.premium.TicketModule")
                                .getConstructor(SoulGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.soulguard.api.SecurityModule) Class
                                .forName("com.soulguard.modules.premium.BotFirewallModule")
                                .getConstructor(SoulGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.soulguard.api.SecurityModule) Class
                                .forName("com.soulguard.modules.premium.PacketObfuscator")
                                .getConstructor(SoulGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.soulguard.api.SecurityModule) Class
                                .forName("com.soulguard.modules.premium.ForensicsReplayModule")
                                .getConstructor(SoulGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.soulguard.api.SecurityModule) Class
                                .forName("com.soulguard.modules.premium.GlobalTrustModule")
                                .getConstructor(SoulGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.soulguard.api.SecurityModule) Class
                                .forName("com.soulguard.modules.premium.PredictiveBanIA")
                                .getConstructor(SoulGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.soulguard.api.SecurityModule) Class
                                .forName("com.soulguard.modules.premium.ShadowMirrorInstance")
                                .getConstructor(SoulGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.soulguard.api.SecurityModule) Class.forName("com.soulguard.modules.premium.ThreatAnalyzer")
                                .getConstructor(SoulGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.soulguard.api.SecurityModule) Class.forName("com.soulguard.modules.premium.HealthAnalyzer")
                                .getConstructor(SoulGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.soulguard.api.SecurityModule) Class
                                .forName("com.soulguard.modules.premium.WeeklyReportModule")
                                .getConstructor(SoulGuard.class).newInstance(this));

                this.moduleManager.registerModule(ModuleGroup.PREMIUM,
                        (com.soulguard.api.SecurityModule) Class.forName("com.soulguard.modules.premium.TraceEngine")
                                .getConstructor(SoulGuard.class).newInstance(this));

                logManager.logInfo("[Premium] Loaded 19 premium modules + 8 advanced checks!");

                // Final Horizon Polish: Pro Config Validator
                new com.soulguard.config.ConfigValidator(this).validate();
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException
                    | java.lang.reflect.InvocationTargetException e) {
                logManager.logError("[Premium] Failed to load perfection-phase modules: " + e.getMessage());
            }
        } else {
            logManager.logInfo("[Lite] Running in Lite mode - Premium features disabled");
            logManager.logInfo("[Lite] Upgrade to Premium for Web Panel, ML Detection, Cloud Blacklist, and more!");
        }
        com.soulguard.util.ConsoleUtil.phase("Module Registration Complete");
    }

    public <T extends SecurityModule> T getModule(Class<T> clazz) {
        return moduleManager.getModule(clazz);
    }

    public StaffPIN getStaffPIN() {
        return getModule(StaffPIN.class);
    }

    public com.soulguard.database.SessionManager getSessionManager() {
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

    public com.soulguard.database.PunishmentManager getPunishmentManager() {
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
     * Gets the edition of SoulGuard currently running.
     * 
     * @return the edition (Lite or Premium)
     */
    public Edition getEdition() {
        return edition;
    }

    /**
     * Detects which edition of SoulGuard is running by checking for premium
     * classes.
     * 
     * @return Edition.PREMIUM if premium classes exist, Edition.LITE otherwise
     */
    private Edition detectEdition() {
        try {
            // Try to load a premium-only class
            Class.forName("com.soulguard.modules.premium.WebPanelModule");
            return Edition.PREMIUM;
        } catch (ClassNotFoundException e) {
            return Edition.LITE;
        }
    }

    @Override
    public void onDisable() {
        if (this.logManager != null) {
            this.logManager.logInfo("Disabling SoulGuard...");
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

    public static SoulGuard getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public com.soulguard.database.RedisManager getRedisManager() {
        return redisManager;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }
}
