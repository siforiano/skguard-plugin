package com.soulguard.commands.framework;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.soulguard.SoulGuard;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final SoulGuard plugin;
    private final Map<String, SubCommand> commands = new HashMap<>();

    public CommandManager(SoulGuard plugin) {
        this.plugin = plugin;
    }

    public void register(SubCommand cmd) {
        commands.put(cmd.getName(), cmd);
    }

    public void register(String label, SubCommand cmd) {
        commands.put(label, cmd);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            boolean isPremium = plugin.getEdition().isPremium();
            String editionPrefix = isPremium ? "&6&lPREMIUM" : "&b&lLITE";
            
            sender.sendMessage(com.soulguard.util.ColorUtil.translate("&8&m----------------------------------------"));
            sender.sendMessage(com.soulguard.util.ColorUtil.translate("&6&lSoulGuard " + editionPrefix + " &7- &fCommand Help"));
            sender.sendMessage("");
            
            commands.values().stream()
                .filter(cmd -> sender.hasPermission(cmd.getPermission()))
                .distinct()
                .forEach(cmd -> {
                    String color = isPremium ? "&e" : "&f";
                    sender.sendMessage(com.soulguard.util.ColorUtil.translate(color + "/sg " + cmd.getName() + " &7- " + cmd.getDescription()));
                });
            
            if (!isPremium) {
                sender.sendMessage("");
                sender.sendMessage(com.soulguard.util.ColorUtil.translate("&6⭐ &e&lUpgrade to PREMIUM &7to unlock all features!"));
            }
            
            sender.sendMessage(com.soulguard.util.ColorUtil.translate("&8&m----------------------------------------"));
            return true;
        }

        SubCommand cmd = commands.get(args[0].toLowerCase());
        if (cmd == null || !sender.hasPermission(cmd.getPermission())) {
            sender.sendMessage(ChatColor.RED + "Unknown command or insufficient permissions. Type /sg help.");
            return true;
        }

        cmd.execute(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String partial = args[0].toLowerCase();
            for (Map.Entry<String, SubCommand> entry : commands.entrySet()) {
                if (entry.getKey().startsWith(partial) && sender.hasPermission(entry.getValue().getPermission())) {
                    completions.add(entry.getKey());
                }
            }
            return completions;
        } else if (args.length > 1) {
            SubCommand cmd = commands.get(args[0].toLowerCase());
            if (cmd != null && sender.hasPermission(cmd.getPermission())) {
                return cmd.tabComplete(sender, args);
            }
        }
        return new ArrayList<>();
    }
}
