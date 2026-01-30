package com.soulguard.modules.staff;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import com.soulguard.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.PermissionAttachment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QuarantineManager implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private final Map<UUID, PermissionAttachment> quarantineAttachments = new HashMap<>();
    private final java.util.Set<UUID> wereOp = new java.util.HashSet<>();

    public QuarantineManager(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "StaffQuarantine";
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
        quarantineAttachments.values().forEach(PermissionAttachment::remove);
        quarantineAttachments.clear();
    }

    @Override
    public void reload() {
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled)
            return;

        // If AuthModule is enabled, we wait until they login to quarantine them (requested sequence)
        SecurityModule auth = plugin.getModuleManager().getModule("AuthModule");
        if (auth != null && auth.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.hasPermission("soulguard.staff")) {
            quarantinePlayer(player);
        }
    }

    public void quarantinePlayer(Player player) {
        if (!enabled || quarantineAttachments.containsKey(player.getUniqueId()))
            return;

        // Strip OP status temporarily
        if (player.isOp()) {
            player.setOp(false);
            wereOp.add(player.getUniqueId());
            plugin.getLogManager().logInfo("Temporarily stripped OP from " + player.getName() + " (Quarantine)");
        }

        // Apply a negative permission attachment to block most things
        // In a real plugin, we might strip group-based permissions if the provider
        // supports it.
        PermissionAttachment attachment = player.addAttachment(plugin);
        attachment.setPermission("minecraft.command.op", false);
        attachment.setPermission("minecraft.command.stop", false);
        attachment.setPermission("soulguard.admin", false);

        quarantineAttachments.put(player.getUniqueId(), attachment);

        player.sendMessage(ColorUtil.translate(
                "&c&l[Security] &7Your administrative powers are quarantined until you pass all security checks."));
    }

    public void releasePlayer(Player player) {
        PermissionAttachment attachment = quarantineAttachments.remove(player.getUniqueId());
        if (attachment != null) {
            attachment.remove();

            // Restore OP if they were OP before
            if (wereOp.remove(player.getUniqueId())) {
                player.setOp(true);
                plugin.getLogManager().logInfo("Restored OP status for " + player.getName());
            }

            player.sendMessage(ColorUtil.translate("&a&l[Security] &fAll security checks passed. Powers restored."));
            plugin.getLogManager().logInfo("Released " + player.getName() + " from Quarantine.");
        }
    }
}
