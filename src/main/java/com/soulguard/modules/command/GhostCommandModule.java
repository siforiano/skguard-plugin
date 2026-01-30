package com.soulguard.modules.command;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import com.soulguard.util.ColorUtil;

public class GhostCommandModule implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private final List<String> ghostCommands = Arrays.asList("/opme", "/authbypass", "/sg admin bypass",
            "/stop server now", "/hack");

    public GhostCommandModule(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "GhostCommands";
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!enabled)
            return;

        String cmd = event.getMessage().toLowerCase();
        Player player = event.getPlayer();

        // If player is NOT verified and attempts a ghost command
        if (ghostCommands.stream().anyMatch(cmd::startsWith)) {
            event.setCancelled(true);

            plugin.getLogManager().logWarn("Ghost Command Triggered by " + player.getName() + " -> " + cmd);
            plugin.getAuditManager().logAction(player, "GHOST_COMMAND_TRIGGER", "Player tried: " + cmd);

            player.kickPlayer(
                    ColorUtil.translate("&c&l[SoulGuard] &7Security Violation: Unauthorized command access."));
        }
    }
}
