package com.skguard.commands.sub;

import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.skguard.SKGuard;
import com.skguard.commands.framework.SubCommand;
import com.skguard.util.SecurityPresets;

public class SetupCommand implements SubCommand {

    private final SKGuard plugin;

    public SetupCommand(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "setup";
    }

    @Override
    public String getPermission() {
        return "SKGuard.admin";
    }

    @Override
    public String getDescription() {
        return "Guided security setup wizard.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.GOLD + "█ SKGuard Setup Wizard");
            sender.sendMessage(ChatColor.GRAY + "Select a security preset to automatically configure the plugin:");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.YELLOW + "/sg setup LOW" + ChatColor.WHITE + " - Basic protection, high compatibility.");
            sender.sendMessage(ChatColor.GREEN + "/sg setup STANDARD" + ChatColor.WHITE + " - Balanced (Recommended).");
            sender.sendMessage(ChatColor.RED + "/sg setup STRICT" + ChatColor.WHITE + " - High security, strict checks.");
            sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "/sg setup PARANOID" + ChatColor.WHITE + " - Maximum lockdown mode.");
            return;
        }

        try {
            SecurityPresets.Preset preset = SecurityPresets.Preset.valueOf(args[1].toUpperCase());
            SecurityPresets.applyPreset(plugin, preset);
            sender.sendMessage(ChatColor.GREEN + "[SKGuard] Preset " + preset.name() + " applied successfully!");
            sender.sendMessage(ChatColor.GRAY + "Modules have been reloaded with the new security levels.");
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid preset! Use LOW, STANDARD, STRICT, or PARANOID.");
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("LOW", "STANDARD", "STRICT", "PARANOID");
        }
        return java.util.Collections.emptyList();
    }
}

