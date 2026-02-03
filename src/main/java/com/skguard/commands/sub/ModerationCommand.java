package com.skguard.commands.sub;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.skguard.SKGuard;
import com.skguard.commands.framework.SubCommand;
import com.skguard.database.PunishmentManager.PunishmentType;
import com.skguard.modules.auth.UserSecurityModule;
import com.skguard.util.ColorUtil;

@SuppressWarnings("deprecation")
public class ModerationCommand implements SubCommand {

    private final SKGuard plugin;
    private final String name;

    public ModerationCommand(SKGuard plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPermission() {
        return "SKGuard.staff";
    }

    @Override
    public String getDescription() {
        return "Moderation utilities.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender == null || args == null || args.length == 0) return;
        String sub = args[0].toLowerCase();

        switch (sub) {
            case "warn" -> handleWarn(sender, args);
            case "unban" -> handleUnpunish(sender, args, PunishmentType.BAN);
            case "unmute" -> handleUnpunish(sender, args, PunishmentType.MUTE);
            case "history" -> handleHistory(sender, args);
            case "alts" -> handleAlts(sender, args);
            case "lock" -> handleLock(sender, args, true);
            case "unlock" -> handleLock(sender, args, false);
        }
    }

    private void handleWarn(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg warn <player> <reason>");
            return;
        }
        org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
        StringBuilder reason = new StringBuilder();
        for (int i = 2; i < args.length; i++) reason.append(args[i]).append(" ");
        String reasonStr = reason.toString().trim();

        String targetIp = "0.0.0.0";
        if (target.isOnline()) {
            org.bukkit.entity.Player pOnline = target.getPlayer();
            if (pOnline != null) {
                java.net.InetSocketAddress isa = pOnline.getAddress();
                if (isa != null) {
                    java.net.InetAddress ia = isa.getAddress();
                    if (ia != null) {
                        targetIp = ia.getHostAddress();
                    }
                }
            }
        }
        com.skguard.database.PunishmentManager.Punishment p = new com.skguard.database.PunishmentManager.Punishment(
                target.getUniqueId(), targetIp, PunishmentType.WARN, reasonStr, sender.getName(), System.currentTimeMillis(), 0L, true);
        
        plugin.getPunishmentManager().addPunishment(p).thenRun(() -> {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (target.isOnline()) {
                    Player pOnline = target.getPlayer();
                    if (pOnline != null) {
                        pOnline.sendMessage(ColorUtil.translate("&c&l[SKGuard] &7WARNING: &f" + reasonStr));
                    }
                }
                String targetName = target.getName() != null ? target.getName() : args[1];
                sender.sendMessage(ChatColor.GREEN + "Warning issued to " + targetName);
            });
        });
    }

    private void handleUnpunish(CommandSender sender, String[] args, PunishmentType type) {
        if (!sender.hasPermission("SKGuard.admin")) {
            sender.sendMessage(ChatColor.RED + "No perm.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg un" + type.name().toLowerCase() + " <player>");
            return;
        }
        org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
        plugin.getPunishmentManager().deactivatePunishment(target.getUniqueId(), type).thenRun(() -> {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                String targetName = target.getName() != null ? target.getName() : args[1];
                sender.sendMessage(ChatColor.GREEN + targetName + " has been un" + type.name().toLowerCase() + "ed.");
            });
        });
    }

    private void handleHistory(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg history <player>");
            return;
        }
        org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
        plugin.getPunishmentManager().getHistory(target.getUniqueId()).thenAccept(history -> {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                String targetName = target.getName() != null ? target.getName() : args[1];
                sender.sendMessage(ChatColor.GOLD + "--- Sanction History for " + targetName + " ---");
                if (history.isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "No sanctions found.");
                    return;
                }
                for (com.skguard.database.PunishmentManager.Punishment p : history) {
                    String status = p.active ? ChatColor.RED + "[ACTIVE]" : ChatColor.GRAY + "[EXPIRED]";
                    String time = new java.text.SimpleDateFormat("dd/MM HH:mm").format(new java.util.Date(p.issuedAt));
                    sender.sendMessage(status + ChatColor.YELLOW + " " + p.type.name() + ChatColor.RESET + " (" + time + ") by " + p.operator);
                    sender.sendMessage(ChatColor.GRAY + " Reason: " + ChatColor.WHITE + p.reason);
                }
            });
        });
    }

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
        Set<UUID> alts = plugin.getAltLinker().getAlts(target.getUniqueId());
        String targetName = target.getName() != null ? target.getName() : args[1];
        sender.sendMessage(ChatColor.GOLD + "--- Alt Accounts for " + targetName + " ---");
        if (alts.size() <= 1) {
            sender.sendMessage(ChatColor.GRAY + "No alt accounts found.");
            return;
        }
        for (UUID uuid : alts) {
            org.bukkit.OfflinePlayer alt = org.bukkit.Bukkit.getOfflinePlayer(uuid);
            String altName = alt.getName() != null ? alt.getName() : uuid.toString();
            String status = alt.isOnline() ? ChatColor.GREEN + "[Online]" : ChatColor.GRAY + "[Offline]";
            sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + altName + " " + status);
        }
    }

    private void handleLock(CommandSender sender, String[] args, boolean lock) {
        if (!sender.hasPermission("SKGuard.admin")) return;
        if (args.length < 2 && !lock) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg unlock <player>");
            return;
        }
        
        // Special case for lock (self or others)
        if (lock && args.length == 1) {
            if (!(sender instanceof Player p)) {
                 sender.sendMessage("Provide player."); return;
            }
            UserSecurityModule us = plugin.getUserSecurity();
            if (us != null) {
                us.setLocked(p.getUniqueId(), true);
                p.kickPlayer(ColorUtil.translate("&cLocked."));
            }
            return;
        }

        org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
        if (plugin.getUserSecurity() != null) {
            plugin.getUserSecurity().setLocked(target.getUniqueId(), lock);
        if (lock && target.isOnline()) {
            Player targetPlayer = target.getPlayer();
            if (targetPlayer != null) {
                targetPlayer.kickPlayer(ColorUtil.translate("&cAccount manually LOCKED."));
            }
        }
            String targetName = target.getName() != null ? target.getName() : args[1];
            sender.sendMessage(ChatColor.GREEN + "Account " + (lock ? "LOCKED" : "UNLOCKED") + " for " + targetName);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}

