package com.skguard.commands.sub;

import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.skguard.SKGuard;
import com.skguard.commands.framework.SubCommand;
import com.skguard.integration.DiscordWebhook;
import com.skguard.util.ColorUtil;

public class SystemCommand implements SubCommand {

    private final SKGuard plugin;
    private final String name;

    public SystemCommand(SKGuard plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPermission() {
        return "SKGuard.admin";
    }

    @Override
    public String getDescription() {
        return "System administration command.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender == null || args == null || args.length == 0) return;
        String sub = args[0].toLowerCase();
        
        switch (sub) {
            case "reload" -> {
                plugin.getConfigManager().reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "[SKGuard] Configuration and modules reloaded.");
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
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /SKGuard audit <query> [limit]");
                    return;
                }
                int limit = 10;
                if (args.length >= 3) {
                    try {
                        limit = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ignored) {}
                }
                if (plugin.getAuditManager() != null) {
                    plugin.getAuditManager().searchLogs(player, args[1], limit);
                }
            }
            case "panic" -> {
                if (plugin.getPanicModule() != null) {
                    boolean newState = !plugin.getPanicModule().isPanicActive();
                    plugin.getPanicModule().setPanicActive(newState);
                    sender.sendMessage(ColorUtil.translate("&c[SKGuard] Panic mode is now " + (newState ? "&lENABLED" : "&lDISABLED")));
                }
            }
            case "maintenance" -> {
                if (plugin.getMaintenanceModule() != null) {
                    boolean maintState = !plugin.getMaintenanceModule().isMaintenanceActive();
                    plugin.getMaintenanceModule().setMaintenanceActive(maintState);
                    sender.sendMessage(ColorUtil.translate("&e[SKGuard] Maintenance mode is now " + (maintState ? "&lENABLED" : "&lDISABLED")));
                }
            }
            case "health" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command is for players only.");
                    return;
                }
                com.skguard.modules.premium.HealthAnalyzer analyzer = plugin.getModule(com.skguard.modules.premium.HealthAnalyzer.class);
                if (analyzer != null) {
                    analyzer.sendReport(player);
                } else {
                    sender.sendMessage(ChatColor.RED + "Health Analyzer module is disabled or not found.");
                }
            }
            case "info" -> {
                String version = plugin.getDescription().getVersion();
                sender.sendMessage(ChatColor.DARK_AQUA + "█▀▀ " + ChatColor.AQUA + "" + ChatColor.BOLD + "SKGuard Security" + ChatColor.DARK_GRAY + " v" + version);
                sender.sendMessage(ChatColor.GRAY + "    " + ChatColor.ITALIC + "Protecting your server with advanced heuristics.");
                sender.sendMessage("");
                sender.sendMessage(ChatColor.GOLD + "» " + ChatColor.WHITE + "Auth & Identity");
                sender.sendMessage(ChatColor.DARK_GRAY + "  ▪ " + ChatColor.GRAY + "Pin, Premium Auto-Login, Alt-Scanner");
                sender.sendMessage(ChatColor.GOLD + "» " + ChatColor.WHITE + "Anti-Bot & World");
                sender.sendMessage(ChatColor.DARK_GRAY + "  ▪ " + ChatColor.GRAY + "Behavior Track, Captcha, Anti-Exploit");
                sender.sendMessage(ChatColor.GOLD + "» " + ChatColor.WHITE + "Moderation");
                sender.sendMessage(ChatColor.DARK_GRAY + "  ▪ " + ChatColor.GRAY + "Ban/Mute System, History, Audit Logs");
                sender.sendMessage("");
                sender.sendMessage(ChatColor.GOLD + "» " + ChatColor.WHITE + "System");
                sender.sendMessage(ChatColor.DARK_GRAY + "  ▪ " + ChatColor.GRAY + "Toggle, Conflicts, Panic, Maintenance");
                sender.sendMessage("");
                sender.sendMessage(ChatColor.DARK_AQUA + "█▄▄ " + ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/sg help" + ChatColor.GRAY + " for commands.");
            }

            }
        }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}

