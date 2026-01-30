package com.soulguard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.soulguard.integration.DiscordWebhook;
import com.soulguard.logger.AuditManager;
import com.soulguard.modules.auth.UserSecurityModule;
import com.soulguard.util.ColorUtil;

public class SoulGuardCommand implements CommandExecutor, TabCompleter {

    private final SoulGuard plugin;
    private final List<String> subCommands = Arrays.asList("reload", "gui", "audit", "panic", "maintenance", "lock",
            "unlock", "testwebhook", "ban", "tempban", "unban", "mute", "tempmute", "unmute", "warn", "history", "session", "alts",
            "info", "help");

    public SoulGuardCommand(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender == null)
            return true;

        if (!sender.hasPermission("soulguard.admin") && !sender.isOp()) {
            if (args.length > 0 && args[0].equalsIgnoreCase("lock")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("This command is for players only.");
                    return true;
                }
                // Allow players to lock themselves even without admin permission
                // (Self-protection)
                Player p = (Player) sender;
                UserSecurityModule userSecurity = plugin.getUserSecurity();
                if (userSecurity != null) {
                    userSecurity.setLocked(p.getUniqueId(), true);
                    p.kickPlayer(ColorUtil.translate("&c&l[Security] &7Account manually LOCKED."));
                }
                return true;
            }
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.getConfigManager().reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "[SoulGuard] Configuration and modules reloaded.");
                plugin.getLogManager().logInfo("Configuration reloaded by " + sender.getName());
            }
            case "testwebhook" -> {
                DiscordWebhook webhook = plugin.getDiscordWebhook();
                if (webhook != null && webhook.isEnabled()) {
                    webhook.sendAlert("Test Alert", "This is a test message from " + sender.getName(), 65280);
                    sender.sendMessage(ChatColor.GREEN + "Test webhook sent!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Discord Webhook is disabled or not configured.");
                }
            }
            case "audit" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command is for players only.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /soulguard audit <query> [limit]");
                    return true;
                }
                int limit = 10;
                if (args.length >= 3) {
                    try {
                        limit = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (plugin.getAuditManager() != null) {
                    plugin.getAuditManager().searchLogs(player, args[1], limit);
                }
            }
            case "panic" -> {
                if (plugin.getPanicModule() != null) {
                    boolean newState = !plugin.getPanicModule().isPanicActive();
                    plugin.getPanicModule().setPanicActive(newState);
                    sender.sendMessage(ColorUtil
                            .translate("&c[SoulGuard] Panic mode is now " + (newState ? "&lENABLED" : "&lDISABLED")));
                }
            }
            case "maintenance" -> {
                if (plugin.getMaintenanceModule() != null) {
                    boolean maintState = !plugin.getMaintenanceModule().isMaintenanceActive();
                    plugin.getMaintenanceModule().setMaintenanceActive(maintState);
                    sender.sendMessage(ColorUtil.translate(
                            "&e[SoulGuard] Maintenance mode is now " + (maintState ? "&lENABLED" : "&lDISABLED")));
                }
            }
            case "gui" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command is for players only.");
                    return true;
                }
                if (plugin.getMenuManager() != null) {
                    plugin.getMenuManager().openMainMenu(player);
                }
            }
            case "debug" -> {
                sender.sendMessage(ChatColor.GOLD + "--- SoulGuard Debug ---");
                String url = plugin.getConfig().getString("modules.DiscordWebhook.url");
                sender.sendMessage(ChatColor.YELLOW + "Webhook URL: " + ChatColor.RESET +
                        (url == null || url.isEmpty() ? ChatColor.RED + "EMPTY" : ChatColor.GREEN + "LOADED"));
                DiscordWebhook webhook = plugin.getDiscordWebhook();
                sender.sendMessage(ChatColor.YELLOW + "Webhook Module: " + ChatColor.RESET +
                        (webhook != null && webhook.isEnabled() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
                if (sender instanceof Player p) {
                    sender.sendMessage(ChatColor.YELLOW + "Your Perm (soulguard.staff): " + ChatColor.RESET
                            + p.hasPermission("soulguard.staff"));
                    sender.sendMessage(ChatColor.YELLOW + "Your Status (Pending PIN): " + ChatColor.RESET
                            + (plugin.getStaffPIN() != null && plugin.getStaffPIN().isPending(p.getUniqueId())));
                }
            }
            case "ban" -> handleBan(sender, args, false);
            case "tempban" -> handleBan(sender, args, true);
            case "unban" -> handleUnpunish(sender, args, com.soulguard.database.PunishmentManager.PunishmentType.BAN);
            case "mute" -> handleMute(sender, args, false);
            case "tempmute" -> handleMute(sender, args, true);
            case "unmute" -> handleUnpunish(sender, args, com.soulguard.database.PunishmentManager.PunishmentType.MUTE);
            case "warn" -> handleWarn(sender, args);
            case "history" -> handleHistory(sender, args);
            case "session" -> handleSession(sender, args);
            case "alts" -> handleAlts(sender, args);
            case "unlock" -> handleUnlock(sender, args);
            case "info" -> sendInfo(sender);
            case "help" -> sendHelp(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        }
        return completions;
    }

    @SuppressWarnings("deprecation")
    private void handleBan(CommandSender sender, String[] args, boolean temp) {
        if (args.length < (temp ? 3 : 2)) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg "
                    + (temp ? "tempban <player> <time> [reason]" : "ban <player> [reason]"));
            return;
        }

        org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
        long expiry = 0;
        String reasonIdx = "2";

        if (temp) {
            expiry = System.currentTimeMillis() + com.soulguard.util.TimeUtil.parseDuration(args[2]);
            if (expiry == System.currentTimeMillis()) {
                sender.sendMessage(ChatColor.RED + "Invalid duration format (e.g. 1h, 30m, 1d)");
                return;
            }
            reasonIdx = "3";
        }

        StringBuilder reason = new StringBuilder();
        int start = Integer.parseInt(reasonIdx);
        for (int i = start; i < args.length; i++)
            reason.append(args[i]).append(" ");
        String reasonStr = reason.length() == 0 ? "Banned by an operator." : reason.toString().trim();

        final long finalExpiry = expiry;
        String targetIp = "0.0.0.0";
        if (target.isOnline()) {
            Player pOnline = target.getPlayer();
            if (pOnline != null && pOnline.getAddress() != null) {
                targetIp = pOnline.getAddress().getAddress().getHostAddress();
            }
        }
        com.soulguard.database.PunishmentManager.Punishment p = new com.soulguard.database.PunishmentManager.Punishment(
                target.getUniqueId(), targetIp, com.soulguard.database.PunishmentManager.PunishmentType.BAN, reasonStr,
                sender.getName(), System.currentTimeMillis(), finalExpiry, true);

        plugin.getPunishmentManager().addPunishment(p).thenRun(() -> {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (target.isOnline()) {
                    Player pOnline = target.getPlayer();
                    if (pOnline != null) {
                        String timeStr = finalExpiry > 0 ? " for " + args[2] : "";
                        pOnline.kickPlayer(ColorUtil.translate(
                                "&c&l[SoulGuard] &7You have been BANNED" + timeStr + "\n&fReason: &7" + reasonStr));
                    }
                }
                sender.sendMessage(ChatColor.GREEN + target.getName() + " has been banned.");
            });
        });
    }

    @SuppressWarnings("deprecation")
    private void handleMute(CommandSender sender, String[] args, boolean temp) {
        if (args.length < (temp ? 3 : 2)) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg "
                    + (temp ? "tempmute <player> <time> [reason]" : "mute <player> [reason]"));
            return;
        }

        org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
        long expiry = 0;
        String reasonIdx = "2";

        if (temp) {
            expiry = System.currentTimeMillis() + com.soulguard.util.TimeUtil.parseDuration(args[2]);
            if (expiry == System.currentTimeMillis()) {
                sender.sendMessage(ChatColor.RED + "Invalid duration format.");
                return;
            }
            reasonIdx = "3";
        }

        StringBuilder reason = new StringBuilder();
        int start = Integer.parseInt(reasonIdx);
        for (int i = start; i < args.length; i++)
            reason.append(args[i]).append(" ");
        String reasonStr = reason.length() == 0 ? "Muted by an operator." : reason.toString().trim();

        final long finalExpiry = expiry;
        String targetIp = "0.0.0.0";
        if (target.isOnline()) {
            Player pOnline = target.getPlayer();
            if (pOnline != null && pOnline.getAddress() != null) {
                targetIp = pOnline.getAddress().getAddress().getHostAddress();
            }
        }
        com.soulguard.database.PunishmentManager.Punishment p = new com.soulguard.database.PunishmentManager.Punishment(
                target.getUniqueId(), targetIp, com.soulguard.database.PunishmentManager.PunishmentType.MUTE, reasonStr,
                sender.getName(), System.currentTimeMillis(), finalExpiry, true);

        plugin.getPunishmentManager().addPunishment(p).thenRun(() -> {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GREEN + target.getName() + " has been muted.");
            });
        });
    }

    @SuppressWarnings("deprecation") // For getOfflinePlayer(String)
    private void handleUnpunish(CommandSender sender, String[] args,
            com.soulguard.database.PunishmentManager.PunishmentType type) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg un" + type.name().toLowerCase() + " <player>");
            return;
        }
        org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
        plugin.getPunishmentManager().deactivatePunishment(target.getUniqueId(), type).thenRun(() -> {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(
                        ChatColor.GREEN + target.getName() + " has been un" + type.name().toLowerCase() + "ed.");
            });
        });
    }

    @SuppressWarnings("deprecation")
    private void handleWarn(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg warn <player> <reason>");
            return;
        }
        org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
        StringBuilder reason = new StringBuilder();
        for (int i = 2; i < args.length; i++)
            reason.append(args[i]).append(" ");
        String reasonStr = reason.toString().trim();

        String targetIp = "0.0.0.0";
        if (target.isOnline()) {
            Player pOnline = target.getPlayer();
            if (pOnline != null && pOnline.getAddress() != null) {
                targetIp = pOnline.getAddress().getAddress().getHostAddress();
            }
        }
        com.soulguard.database.PunishmentManager.Punishment p = new com.soulguard.database.PunishmentManager.Punishment(
                target.getUniqueId(), targetIp, com.soulguard.database.PunishmentManager.PunishmentType.WARN, reasonStr,
                sender.getName(), System.currentTimeMillis(), 0L, true);

        plugin.getPunishmentManager().addPunishment(p).thenRun(() -> {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (target.isOnline()) {
                    Player pOnline = target.getPlayer();
                    if (pOnline != null) {
                        pOnline.sendMessage(ColorUtil.translate("&c&l[SoulGuard] &7WARNING: &f" + reasonStr));
                    }
                }
                sender.sendMessage(ChatColor.GREEN + "Warning issued to " + target.getName());
            });
        });
    }

    @SuppressWarnings("deprecation")
    private void handleHistory(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg history <player>");
            return;
        }
        org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
        plugin.getPunishmentManager().getHistory(target.getUniqueId()).thenAccept(history -> {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GOLD + "--- Sanction History for " + target.getName() + " ---");
                if (history.isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "No sanctions found.");
                    return;
                }

                for (com.soulguard.database.PunishmentManager.Punishment p : history) {
                    String status = p.active ? ChatColor.RED + "[ACTIVE]" : ChatColor.GRAY + "[EXPIRED]";
                    String time = new java.text.SimpleDateFormat("dd/MM HH:mm").format(new java.util.Date(p.issuedAt));
                    sender.sendMessage(status + ChatColor.YELLOW + " " + p.type.name() + ChatColor.RESET + " (" + time
                            + ") by " + p.operator);
                    sender.sendMessage(ChatColor.GRAY + " Reason: " + ChatColor.WHITE + p.reason);
                }
            });
        });
    }

    private void handleSession(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command is for players only.");
            return;
        }

        Player target = player;
        if (args.length > 1) {
            if (!player.hasPermission("soulguard.audit.full") && !player.isOp()) {
                player.sendMessage(ColorUtil.translate("&c&l[Security] &7Privacy Policy: Access to external session data denied. &8(SEC-AUD-001)"));
                return;
            }
            target = org.bukkit.Bukkit.getPlayer(args[1]);
        }

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        AuditManager audit = plugin.getAuditManager();
        if (audit == null) return;

        String masterKey = plugin.getConfig().getString("general.master-key", "default_soulguard_key");
        java.util.List<String> history = audit.getDecryptedHistory(target.getUniqueId(), masterKey);

        player.sendMessage(ColorUtil.translate("&6&l[SoulGuard] &7Session History for &f" + target.getName()));
        if (history.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No commands recorded in this session.");
        } else {
            for (String cmd : history) {
                player.sendMessage(ColorUtil.translate(" &8» &f" + cmd));
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void handleAlts(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg alts <player>");
            return;
        }
        org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
        if (plugin.getAltLinker() == null) {
            sender.sendMessage(ChatColor.RED + "AltLinker module is disabled.");
            return;
        }

        java.util.Set<java.util.UUID> alts = plugin.getAltLinker().getAlts(target.getUniqueId());
        sender.sendMessage(ChatColor.GOLD + "--- Alt Accounts for " + target.getName() + " ---");
        if (alts.size() <= 1) {
            sender.sendMessage(ChatColor.GRAY + "No alt accounts found.");
            return;
        }

        for (java.util.UUID uuid : alts) {
            org.bukkit.OfflinePlayer alt = org.bukkit.Bukkit.getOfflinePlayer(uuid);
            String status = alt.isOnline() ? ChatColor.GREEN + "[Online]" : ChatColor.GRAY + "[Offline]";
            sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + alt.getName() + " " + status);
        }

    }

    @SuppressWarnings("deprecation")
    private void handleUnlock(CommandSender sender, String[] args) {
        if (!sender.hasPermission("soulguard.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg unlock <player>");
            return;
        }
        org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
        if (plugin.getUserSecurity() != null) {
            plugin.getUserSecurity().setLocked(target.getUniqueId(), false);
            sender.sendMessage(ChatColor.GREEN + "Account unlocked for " + target.getName());
        } else {
            sender.sendMessage(ChatColor.RED + "UserSecurity module is disabled.");
        }
    }

    private void sendInfo(CommandSender sender) {
        String version = plugin.getDescription().getVersion();
        sender.sendMessage(ChatColor.DARK_AQUA + "█▀▀ " + ChatColor.AQUA + "" + ChatColor.BOLD + "SoulGuard Security"
                + ChatColor.DARK_GRAY + " v" + version);
        sender.sendMessage(
                ChatColor.GRAY + "    " + ChatColor.ITALIC + "Protecting your server with advanced heuristics.");
        sender.sendMessage("");

        sender.sendMessage(ChatColor.GOLD + "» " + ChatColor.WHITE + "Auth & Identity");
        sender.sendMessage(ChatColor.DARK_GRAY + "  ▪ " + ChatColor.GRAY + "Pin, Premium Auto-Login, Alt-Scanner");

        sender.sendMessage(ChatColor.GOLD + "» " + ChatColor.WHITE + "Anti-Bot & World");
        sender.sendMessage(ChatColor.DARK_GRAY + "  ▪ " + ChatColor.GRAY + "Behavior Track, Captcha, Anti-Exploit");

        sender.sendMessage(ChatColor.GOLD + "» " + ChatColor.WHITE + "Moderation");
        sender.sendMessage(ChatColor.DARK_GRAY + "  ▪ " + ChatColor.GRAY + "Ban/Mute System, History, Audit Logs");

        sender.sendMessage("");
        sender.sendMessage(ChatColor.DARK_AQUA + "█▄▄ " + ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/sg help"
                + ChatColor.GRAY + " for commands.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- SoulGuard Help ---");
        sender.sendMessage(ChatColor.YELLOW + "/sg info " + ChatColor.RESET + "- View features");
        sender.sendMessage(ChatColor.YELLOW + "/sg gui " + ChatColor.RESET + "- Open Main Menu");
        sender.sendMessage(ChatColor.YELLOW + "/sg panic " + ChatColor.RESET + "- Lockdown Server");
        sender.sendMessage(ChatColor.YELLOW + "/sg maintenance " + ChatColor.RESET + "- Maintenance Mode");
        sender.sendMessage(ChatColor.YELLOW + "/sg lock " + ChatColor.RESET + "/ " + ChatColor.YELLOW
                + "unlock <player>" + ChatColor.RESET + "- Account Lock");
        sender.sendMessage(ChatColor.YELLOW + "/sg audit <player/val>" + ChatColor.RESET + "- Check Logs");
        sender.sendMessage(ChatColor.YELLOW + "/sg history <player>" + ChatColor.RESET + "- View Punishments");
        sender.sendMessage(ChatColor.YELLOW + "/sg alts <player>" + ChatColor.RESET + "- View Alt Accounts");
        sender.sendMessage(ChatColor.YELLOW + "/sg ban/tempban <player> ..." + ChatColor.RESET + "- Ban User");
        sender.sendMessage(ChatColor.YELLOW + "/sg mute/tempmute <player> ..." + ChatColor.RESET + "- Mute User");
        sender.sendMessage(ChatColor.YELLOW + "/sg unban/unmute <player>" + ChatColor.RESET + "- Pardon User");
        sender.sendMessage(ChatColor.YELLOW + "/sg warn <player> <reason>" + ChatColor.RESET + "- Warn User");
        sender.sendMessage(ChatColor.YELLOW + "/sg reload " + ChatColor.RESET + "- Reload Config");
        sender.sendMessage(ChatColor.GRAY + "Shortcuts: /report <player>, /pin, /premium");
        sender.sendMessage(ChatColor.GOLD + "---------------------------");
    }
}
