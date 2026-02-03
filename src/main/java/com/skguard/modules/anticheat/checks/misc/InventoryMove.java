package com.skguard.modules.anticheat.checks.misc;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.skguard.SKGuard;
import com.skguard.modules.anticheat.Check;
import com.skguard.modules.anticheat.PlayerData;

public class InventoryMove extends Check implements Listener {

    private final Set<UUID> openInventories = new HashSet<>();

    public InventoryMove(SKGuard plugin) {
        super(plugin, "InventoryMove", "misc");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            openInventories.add(event.getWhoClicked().getUniqueId());
        }
    }

    @Override
    public void handleMove(org.bukkit.event.player.PlayerMoveEvent event, PlayerData data) {
        if (!isEnabled())
            return;
        if (!openInventories.contains(event.getPlayer().getUniqueId()))
            return;
        org.bukkit.Location to = event.getTo();
        if (to == null)
            return;

        double deltaX = Math.abs(to.getX() - event.getFrom().getX());
        double deltaZ = Math.abs(to.getZ() - event.getFrom().getZ());

        if (deltaX > 0.1 || deltaZ > 0.1) {
            flag(event.getPlayer(), "Moving with open inventory", 0.7);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        openInventories.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        openInventories.remove(event.getPlayer().getUniqueId());
    }
}

