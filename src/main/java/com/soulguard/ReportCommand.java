package com.soulguard;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.soulguard.modules.report.ReportModule;
import com.soulguard.util.ColorUtil;

public class ReportCommand implements CommandExecutor, TabCompleter {

    private final SoulGuard plugin;

    public ReportCommand(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender == null || !(sender instanceof Player)) {
            if (sender != null) sender.sendMessage("This command is for players only.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length < 2) {
            player.sendMessage(ColorUtil.translate("&cUsage: /report <player> <reason>"));
            return true;
        }

        String target = args[0];
        StringBuilder reason = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            reason.append(args[i]).append(" ");
        }

        ReportModule reports = plugin.getReportModule();
        if (reports != null) {
            reports.handleReport(player, target, reason.toString().trim());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
        }
        return completions;
    }
}
