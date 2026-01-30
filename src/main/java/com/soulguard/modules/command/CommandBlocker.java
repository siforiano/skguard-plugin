package com.soulguard.modules.command;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;

public class CommandBlocker implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private final Set<String> blockedCommands;
    private boolean blockSyntax;

    public CommandBlocker(SoulGuard plugin) {
        this.plugin = plugin;
        this.blockedCommands = new HashSet<>();
    }

    @Override
    public String getName() {
        return "CommandBlocker";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        reload();
    }

    @Override
    public void disable() {
        this.enabled = false;
        this.blockedCommands.clear();
    }

    @Override
    public void reload() {
        this.blockedCommands.clear();
        List<String> cmds = plugin.getConfig().getStringList("modules.CommandBlocker.blocked-commands");
        for (String cmd : cmds) {
            if (cmd != null) this.blockedCommands.add(cmd.toLowerCase());
        }
        this.blockSyntax = plugin.getConfig().getBoolean("modules.CommandBlocker.block-plugin-syntax", true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (!enabled)
            return;
        Player player = event.getPlayer();
        if (player.hasPermission("soulguard.bypass.commands"))
            return;

        String message = event.getMessage().toLowerCase();
        String[] parts = message.split(" ");
        if (parts.length == 0 || parts[0].length() < 2) return;
        String command = parts[0].substring(1);

        String blockMsg = plugin.getLanguageManager().getMessage("general.unknown-command");

        // 1. Block Syntax (plugin:command)
        if (blockSyntax && command.contains(":")) {
            event.setCancelled(true);
            player.sendMessage(blockMsg);
            plugin.getLogManager().logWarn("Player " + player.getName() + " tried syntax bypass: " + message);
            if (plugin.getDiscordWebhook() != null) {
                plugin.getDiscordWebhook().sendAlert("Command Warning",
                        "Player **" + player.getName() + "** tried illegal syntax: `" + message + "`", 16711680);
            }
            return;
        }

        // 2. Block Blacklisted Commands
        if (blockedCommands.contains(command)) {
            event.setCancelled(true);
            player.sendMessage(blockMsg);
            plugin.getLogManager().logWarn("Player " + player.getName() + " tried blocked command: " + message);
            if (plugin.getDiscordWebhook() != null) {
                plugin.getDiscordWebhook().sendAlert("Blocked Command",
                        "Player **" + player.getName() + "** tried blocked command: `" + message + "`", 16711680);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabComplete(PlayerCommandSendEvent event) {
        if (!enabled)
            return;
        if (event.getPlayer().hasPermission("soulguard.bypass.commands"))
            return;

        event.getCommands().removeIf(cmd -> {
            String lowerCmd = cmd.toLowerCase();
            return (blockSyntax && lowerCmd.contains(":")) || blockedCommands.contains(lowerCmd);
        });
    }
}
