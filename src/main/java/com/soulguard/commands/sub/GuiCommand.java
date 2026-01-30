package com.soulguard.commands.sub;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.soulguard.SoulGuard;
import com.soulguard.commands.framework.SubCommand;

public class GuiCommand implements SubCommand {

    private final SoulGuard plugin;

    public GuiCommand(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "gui";
    }

    @Override
    public String getPermission() {
        return "soulguard.admin";
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
