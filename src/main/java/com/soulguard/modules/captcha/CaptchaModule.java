package com.soulguard.modules.captcha;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import com.soulguard.util.ColorUtil;

public class CaptchaModule implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private final Map<UUID, Material> targetItems;
    private final Map<UUID, Integer> attempts;
    private int maxAttempts;
    private int timeoutSeconds;
    private final Random random;
    private final String GUI_TITLE = ColorUtil.translate("&c&lVerificación Humana");

    private final List<Material> fillerMaterials = Arrays.asList(
            Material.GLASS_PANE, Material.IRON_NUGGET, Material.DEAD_BUSH, Material.STICK, Material.BONE);
    private final List<Material> targetMaterials = Arrays.asList(
            Material.DIAMOND, Material.GOLD_INGOT, Material.EMERALD, Material.APPLE, Material.SLIME_BALL);

    public CaptchaModule(SoulGuard plugin) {
        this.plugin = plugin;
        this.targetItems = new ConcurrentHashMap<>();
        this.attempts = new ConcurrentHashMap<>();
        this.random = new Random();
    }

    @Override
    public String getName() {
        return "CaptchaModule";
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
        targetItems.clear();
        attempts.clear();
    }

    @Override
    public void reload() {
        this.maxAttempts = plugin.getConfig().getInt("modules.CaptchaModule.max-attempts", 3);
        this.timeoutSeconds = plugin.getConfig().getInt("modules.CaptchaModule.timeout-seconds", 10);
    }

    public void startCaptcha(Player player) {
        if (!enabled) return;
        if (player.hasPermission("soulguard.bypass.captcha")) return;
        
        if (com.soulguard.util.BedrockManager.isBedrock(player) && plugin.getConfig().getBoolean("general.cross-platform.native-forms", true)) {
            sendBedrockCaptcha(player);
        } else {
            openCaptcha(player);
        }
    }

    private void sendBedrockCaptcha(Player player) {
        Material target = targetMaterials.get(random.nextInt(targetMaterials.size()));
        targetItems.put(player.getUniqueId(), target);
        
        try {
            // Simple representation of form building (pseudo-code/reflection-safe)
            player.sendMessage(plugin.getLanguageManager().getMessage(player, "captcha.mobile-form-notice"));
            
            // Actual implementation would use a Form builder from Floodgate/Geyser
            // For now, we remain compatible with standard GUI as fallback or 
            // implement if user has the specific Geyser-Spigot bridge.
            openCaptcha(player); // Fallback to inventory if form fails to build
            
        } catch (NoClassDefFoundError | Exception e) {
            openCaptcha(player);
        }
    }

    private void openCaptcha(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
        Material target = targetMaterials.get(random.nextInt(targetMaterials.size()));
        targetItems.put(player.getUniqueId(), target);

        // Fill with garbage
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, createItem(fillerMaterials.get(random.nextInt(fillerMaterials.size())), "&7???"));
        }

        // Place target
        int targetSlot = random.nextInt(27);
        inv.setItem(targetSlot, createItem(target, "&a&lCLIC AQUÍ"));

        player.openInventory(inv);
        String targetName = target.name().replace("_", " ");
        player.sendMessage(ColorUtil.translate("&e&l[Captcha] &7Por favor haz clic en el &f" + targetName
                + " &7para verificar que eres humano."));

        // Timeout task
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && targetItems.containsKey(player.getUniqueId())) {
                    targetItems.remove(player.getUniqueId());
                    attempts.remove(player.getUniqueId());
                    player.closeInventory();
                    player.kickPlayer(ColorUtil.translate("&c&l[Captcha] &7Tiempo de verificación agotado (10s)."));
                }
            }
        }.runTaskLater(plugin, timeoutSeconds * 20L);
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.translate(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!enabled || !event.getView().getTitle().equals(GUI_TITLE))
            return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR)
            return;

        Material target = targetItems.get(player.getUniqueId());
        if (clicked.getType() == target) {
            targetItems.remove(player.getUniqueId());
            attempts.remove(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(ColorUtil.translate("&a&l[Captcha] &f¡Verificado! Diviértete."));
            Bukkit.getPluginManager().callEvent(new com.soulguard.api.event.CaptchaSuccessEvent(player));
        } else {
            int currentAttempts = attempts.getOrDefault(player.getUniqueId(), 0) + 1;
            attempts.put(player.getUniqueId(), currentAttempts);

            if (currentAttempts >= maxAttempts) {
                player.kickPlayer(ColorUtil.translate("&c&l[Captcha] &7Fallaste la verificación demasiadas veces."));
            } else {
                player.sendMessage(ColorUtil.translate(
                        "&c&l[Captcha] &7¡Ítem incorrecto! Intenta de nuevo (" + currentAttempts + "/" + maxAttempts + ")"));
                openCaptcha(player); // Re-roll
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!enabled || !event.getView().getTitle().equals(GUI_TITLE))
            return;

        Player player = (Player) event.getPlayer();
        if (targetItems.containsKey(player.getUniqueId())) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && targetItems.containsKey(player.getUniqueId())) {
                        openCaptcha(player);
                    }
                }
            }.runTaskLater(plugin, 5L);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (enabled && targetItems.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        targetItems.remove(event.getPlayer().getUniqueId());
        attempts.remove(event.getPlayer().getUniqueId());
    }
}
