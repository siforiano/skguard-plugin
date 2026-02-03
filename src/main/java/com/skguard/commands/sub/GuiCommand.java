package com.skguard.commands.sub;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.skguard.SKGuard;
import com.skguard.commands.framework.SubCommand;

public class GuiCommand implements SubCommand {

    private final SKGuard plugin;

    public GuiCommand(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "gui";
    }

    @Override
    public String getPermission() {
        return "SKGuard.admin";
    }

    @Override
    public String getDescription() {
        return "Open the main menu.";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender == null) return;
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return;
        }
        if (plugin.getMenuManager() != null) {
            plugin.getMenuManager().openMainMenu(player);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}

