package com.soulguard.modules.bot;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import com.soulguard.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class HoneypotModule implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;

    public HoneypotModule(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Honeypot";
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
        // No specific config for now
    }

    public void spawnTrap(Player player) {
        if (!enabled)
            return;

        Location loc = player.getLocation().add(player.getLocation().getDirection().multiply(2));
        ArmorStand stand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);

        stand.setVisible(false);
        stand.setSmall(true);
        stand.setMarker(true); // Don't block movement
        stand.setMetadata("sg_honeypot", new FixedMetadataValue(plugin, true));

        // Remove after 10 seconds
        Bukkit.getScheduler().runTaskLater(plugin, stand::remove, 200L);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!enabled)
            return;

        if (event.getDamager() instanceof Player && event.getEntity().hasMetadata("sg_honeypot")) {
            Player player = (Player) event.getDamager();

            // Bottlers/Killaura bots often attack invisible entities instantly
            plugin.getLogManager().logWarn("Honeypot triggered by " + player.getName() + "!");
            plugin.getAuditManager().logAction(player, "BOT_TRAP_TRIGGERED", "Attacked invisible honeypot entity.");

            // Auto-ban if requested or just kick for now
            player.kickPlayer(ColorUtil.translate("&c[SoulGuard] Bot Detection Triggered (Honeypot)."));
            event.setCancelled(true);
        }
    }
}
