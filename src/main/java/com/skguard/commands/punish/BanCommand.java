package com.skguard.commands.punish;

import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.skguard.SKGuard;
import com.skguard.commands.framework.SubCommand;
import com.skguard.util.ColorUtil;
import com.skguard.util.TimeUtil;

@SuppressWarnings("deprecation")
public class BanCommand implements SubCommand {

    private final SKGuard plugin;

    public BanCommand(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "ban"; // Primary name, but we will register for tempban too manually if needed
    }

    @Override
    public String getPermission() {
        return "SKGuard.admin";
    }
    
    @Override
    public String getDescription() {
        return "Ban a player from the server.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // args[0] is "ban" or "tempban"
        boolean temp = args[0].equalsIgnoreCase("tempban");

        if (args.length < (temp ? 3 : 2)) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg " + (temp ? "tempban <player> <time> [reason]" : "ban <player> [reason]"));
            return;
        }

        org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
        long expiry = 0;
        String reasonIdx = "2";

        if (temp) {
            try {
                expiry = System.currentTimeMillis() + TimeUtil.parseDuration(args[2]);
            } catch (Exception e) {
                 sender.sendMessage(ChatColor.RED + "Invalid duration format (e.g. 1h, 30m).");
                 return;
            }
            if (TimeUtil.parseDuration(args[2]) <= 0) {
                 sender.sendMessage(ChatColor.RED + "Invalid duration.");
                 return;
            }
            reasonIdx = "3";
        }

        StringBuilder reason = new StringBuilder();
        int start = Integer.parseInt(reasonIdx);
        for (int i = start; i < args.length; i++) reason.append(args[i]).append(" ");
        String reasonStr = reason.length() == 0 ? "Banned by an operator." : reason.toString().trim();

        final long finalExpiry = expiry;
        String targetIp = "0.0.0.0";
        if (target.isOnline()) {
            Player pOnline = target.getPlayer();
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
                target.getUniqueId(), targetIp, com.skguard.database.PunishmentManager.PunishmentType.BAN, reasonStr, sender.getName(), System.currentTimeMillis(), finalExpiry, true);
        
        plugin.getPunishmentManager().addPunishment(p).thenRun(() -> {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                if (target.isOnline()) {
                    Player pOnline = target.getPlayer();
                    if (pOnline != null) {
                        String timeStr = finalExpiry > 0 ? " for " + args[2] : "";
                        pOnline.kickPlayer(ColorUtil.translate("&c&l[SKGuard] &7You have been BANNED" + timeStr + "\n&fReason: &7" + reasonStr));
                    }
                }
                sender.sendMessage(ChatColor.GREEN + target.getName() + " has been banned.");
            });
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return null; // Return null to let Bukkit suggest player names
        }
        return Collections.emptyList();
    }
}

