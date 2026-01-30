package com.soulguard.modules.auth;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class InventorySnapshotModule implements SecurityModule {

    private final SoulGuard plugin;
    private boolean enabled;
    private final File snapshotDir;

    public InventorySnapshotModule(SoulGuard plugin) {
        this.plugin = plugin;
        this.snapshotDir = new File(plugin.getDataFolder(), "snapshots");
    }

    @Override
    public String getName() {
        return "InventorySnapshots";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        if (!snapshotDir.exists())
            snapshotDir.mkdirs();
    }

    @Override
    public void disable() {
        this.enabled = false;
    }

    @Override
    public void reload() {
    }

    public void takeSnapshot(Player player, String reason) {
        if (!enabled)
            return;

        String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File file = new File(snapshotDir, player.getName() + "_" + time + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("player", player.getName());
        config.set("uuid", player.getUniqueId().toString());
        config.set("reason", reason);
        config.set("timestamp", time);
        config.set("location", player.getLocation());
        config.set("health", player.getHealth());
        config.set("food", player.getFoodLevel());
        config.set("inventory", player.getInventory().getContents());
        config.set("armor", player.getInventory().getArmorContents());

        try {
            config.save(file);
            plugin.getLogManager().logInfo("Inventory Snapshot saved for " + player.getName() + " Reason: " + reason);
        } catch (IOException e) {
            plugin.getLogManager().logError("Failed to save inventory snapshot: " + e.getMessage());
        }
    }
}
