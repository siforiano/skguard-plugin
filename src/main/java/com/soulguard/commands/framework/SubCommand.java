package com.soulguard.commands.framework;

import java.util.List;

import org.bukkit.command.CommandSender;

public interface SubCommand {
    String getName();
    String getPermission();
    String getDescription();
    void execute(CommandSender sender, String[] args);
    List<String> tabComplete(CommandSender sender, String[] args);
}
