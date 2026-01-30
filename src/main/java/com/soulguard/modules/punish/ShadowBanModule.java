package com.soulguard.modules.punish;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ShadowBanModule implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private final Set<UUID> shadowBanned;

    public ShadowBanModule(SoulGuard plugin) {
        this.plugin = plugin;
        this.shadowBanned = new HashSet<>();
    }

    @Override
    public String getName() {
        return "ShadowBan";
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
        shadowBanned.clear();
    }

    @Override
    public void reload() {
    }

    public void setShadowBanned(UUID uuid, boolean state) {
        if (state)
            shadowBanned.add(uuid);
        else
            shadowBanned.remove(uuid);
    }

    public boolean isShadowBanned(UUID uuid) {
        return shadowBanned.contains(uuid);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!enabled || !shadowBanned.contains(event.getPlayer().getUniqueId()))
            return;

        // Shadow Ban: Only the player sees their own message
        Player player = event.getPlayer();
        String message = String.format(event.getFormat(), player.getDisplayName(), event.getMessage());
        player.sendMessage(message);

        // Prevent everyone else from seeing it
        event.getRecipients().clear();
        event.getRecipients().add(player);

        plugin.getLogManager()
                .logInfo("[ShadowBan] Isolated chat from " + player.getName() + ": " + event.getMessage());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (enabled && shadowBanned.contains(event.getPlayer().getUniqueId())) {
            // Ghosting: Let them think they placed it (handled by client prediction mostly)
            // but cancel it on server side immediately.
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (enabled && shadowBanned.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (enabled && shadowBanned.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
