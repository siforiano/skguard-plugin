package com.skguard.modules.trade;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;
import com.skguard.util.ColorUtil;

public class TradeGuard implements SecurityModule, Listener {

    private final SKGuard plugin;
    private boolean enabled;
    private final Set<Material> highValueItems = new HashSet<>(Arrays.asList(
            Material.DIAMOND, Material.DIAMOND_BLOCK, Material.NETHERITE_INGOT,
            Material.NETHERITE_BLOCK, Material.ELYTRA, Material.ENCHANTED_GOLDEN_APPLE));
    private final Set<UUID> dropConfirmations = new HashSet<>();

    public TradeGuard(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "TradeGuard";
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
        dropConfirmations.clear();
    }

    @Override
    public void reload() {
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (!enabled)
            return;

        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();

        if (isHighValue(item)) {
            if (!dropConfirmations.contains(player.getUniqueId())) {
                event.setCancelled(true);
                dropConfirmations.add(player.getUniqueId());
                player.sendMessage(ColorUtil
                        .translate("&e&l[TradeGuard] &7Are you sure you want to drop this &fvaluable item&7?"));
                player.sendMessage(ColorUtil.translate("&7Drop it again within 5 seconds to confirm."));

                // Clear confirmation after 5 seconds
                plugin.getServer().getScheduler().runTaskLater(plugin,
                        () -> dropConfirmations.remove(player.getUniqueId()), 100L);
            } else {
                dropConfirmations.remove(player.getUniqueId());
                player.sendMessage(ColorUtil.translate("&a&l[TradeGuard] &7Item dropped."));
            }
        }
    }

    private boolean isHighValue(ItemStack item) {
        if (item == null)
            return false;
        if (item.hasItemMeta()) {
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null && (meta.hasDisplayName() || meta.hasEnchants()))
                return true;
        }
        return highValueItems.contains(item.getType());
    }
}

