package com.soulguard;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.soulguard.modules.staff.StaffPIN;

public class PINCommand implements CommandExecutor, TabCompleter {

    private final SoulGuard plugin;

    public PINCommand(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender == null || !(sender instanceof Player)) {
            if (sender != null) sender.sendMessage(ChatColor.RED + "Only players can use the PIN system.");
            return true;
        }

        Player player = (Player) sender;
        StaffPIN pinModule = plugin.getStaffPIN();
        if (pinModule == null || !pinModule.isEnabled()) {
            player.sendMessage(ChatColor.RED + "Staff PIN system is currently disabled.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("change")) {
            if (!player.hasPermission("soulguard.changepin")) {
                player.sendMessage(plugin.getLanguageManager().getMessage("no-permission"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /pin change <new_pin>");
                return true;
            }
            // Logic handled in StaffPIN.handlePinCommand eventually or here
            // Let's delegate to a new method in StaffPIN or handle here
        }

        if (!player.hasPermission("soulguard.staff") && !pinModule.isPending(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use the PIN system.");
            return true;
        }

        return pinModule.handlePinCommand(player, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            if ("set".startsWith(input)) completions.add("set");
            if ("change".startsWith(input)) completions.add("change");
        }
        return completions;
    }
}
