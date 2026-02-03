package com.skguard.modules.command;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;

public class CommandGuard implements SecurityModule, Listener {

    private final SKGuard plugin;
    private boolean enabled;
    private final List<String> sensitiveCommands = Arrays.asList("/op", "/stop", "/reload", "/ban", "/pardon",
            "/gamemode", "/SKGuard");

    public CommandGuard(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "CommandGuard";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
    }

    @Override
    public void disable() {
        this.enabled = false;
    }

    @Override
    public void reload() {
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTabComplete(TabCompleteEvent event) {
        if (!enabled || !(event.getSender() instanceof Player))
            return;

        Player player = (Player) event.getSender();

        // Check if player is verified (PIN and Discord)
        boolean pinVerified = !plugin.getStaffPIN().isEnabled()
                || !plugin.getStaffPIN().isPending(player.getUniqueId());
        boolean discordVerified = !plugin.getDiscordLink().isEnabled()
                || plugin.getDiscordLink().isVerified(player.getUniqueId());

        if (!pinVerified || !discordVerified) {
            // Filter out sensitive commands from completions
            event.getCompletions().removeIf(completion -> {
                String cmd = "/" + completion.toLowerCase();
                return sensitiveCommands.stream().anyMatch(s -> cmd.startsWith(s));
            });
        }
    }
}

