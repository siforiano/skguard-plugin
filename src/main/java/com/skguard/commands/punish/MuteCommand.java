package com.skguard.commands.punish;

import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.skguard.SKGuard;
import com.skguard.commands.framework.SubCommand;
import com.skguard.util.TimeUtil;

@SuppressWarnings("deprecation")
public class MuteCommand implements SubCommand {

    private final SKGuard plugin;

    public MuteCommand(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "mute"; 
    }

    @Override
    public String getPermission() {
        return "SKGuard.staff"; // Assumed permission for mute
    }

    @Override
    public String getDescription() {
        return "Mute a player from chat.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        boolean temp = args[0].equalsIgnoreCase("tempmute");

        if (args.length < (temp ? 3 : 2)) {
            sender.sendMessage(ChatColor.RED + "Usage: /sg " + (temp ? "tempmute <player> <time> [reason]" : "mute <player> [reason]"));
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
        String reasonStr = reason.length() == 0 ? "Muted by an operator." : reason.toString().trim();

        final long finalExpiry = expiry;
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
                target.getUniqueId(), targetIp, com.skguard.database.PunishmentManager.PunishmentType.MUTE, reasonStr, sender.getName(), System.currentTimeMillis(), finalExpiry, true);
        
        plugin.getPunishmentManager().addPunishment(p).thenRun(() -> {
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                String targetName = target.getName() != null ? target.getName() : args[1];
                sender.sendMessage(ChatColor.GREEN + targetName + " has been muted.");
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

