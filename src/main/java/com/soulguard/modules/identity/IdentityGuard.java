package com.soulguard.modules.identity;

import java.io.File;
import java.util.Map;
import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import com.soulguard.util.ColorUtil;

public class IdentityGuard implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private final Map<String, UUID> lastKnownUuid = new java.util.concurrent.ConcurrentHashMap<>();
    private File identitiesFile;
    private org.bukkit.configuration.file.FileConfiguration identitiesConfig;

    public IdentityGuard(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "IdentityGuard";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        loadIdentities();
    }

    @Override
    public void disable() {
        this.enabled = false;
        saveIdentities();
        lastKnownUuid.clear();
    }

    @Override
    public void reload() {
        saveIdentities();
        loadIdentities();
    }

    private void loadIdentities() {
        lastKnownUuid.clear();
        if (plugin.getDatabaseManager().isMySQL()) {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try (java.sql.Connection conn = plugin.getDatabaseManager().getConnection();
                        java.sql.PreparedStatement ps = conn.prepareStatement("SELECT * FROM sg_identities")) {
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            lastKnownUuid.put(rs.getString("username").toLowerCase(),
                                    UUID.fromString(rs.getString("uuid")));
                        }
                    }
                } catch (java.sql.SQLException e) {
                    plugin.getLogManager().logError("Failed to load identities from MySQL: " + e.getMessage());
                }
            });
        } else {
            identitiesFile = new File(plugin.getDataFolder(), "identities.yml");
            if (!identitiesFile.exists()) {
                try {
                    identitiesFile.createNewFile();
                } catch (java.io.IOException e) {
                    plugin.getLogManager().logError("Could not create identities.yml");
                }
            }
            identitiesConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(identitiesFile);
            for (String name : identitiesConfig.getKeys(false)) {
                String uuidStr = identitiesConfig.getString(name);
                if (uuidStr != null) {
                    lastKnownUuid.put(name.toLowerCase(), UUID.fromString(uuidStr));
                }
            }
        }
    }

    private void saveIdentities() {
        if (plugin.getDatabaseManager().isMySQL()) {
            // MySQL updates happen in real-time during login
            return;
        }
        if (identitiesConfig == null || identitiesFile == null)
            return;

        for (Map.Entry<String, UUID> entry : lastKnownUuid.entrySet()) {
            identitiesConfig.set(entry.getKey(), entry.getValue().toString());
        }

        try {
            identitiesConfig.save(identitiesFile);
        } catch (java.io.IOException e) {
            plugin.getLogManager().logError("Could not save identities.yml: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        if (!enabled)
            return;

        String name = event.getName().toLowerCase();
        UUID uuid = event.getUniqueId();

        if (lastKnownUuid.containsKey(name)) {
            if (!lastKnownUuid.get(name).equals(uuid)) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        ColorUtil.translate(
                                "&c[SoulGuard] Identity Mismatch Detected.\n&7Please use your original account."));
                plugin.getLogManager()
                        .logWarn("Identity spoof attempt blocked: " + name + " tried to join with a new UUID.");
            }
        } else {
            lastKnownUuid.put(name, uuid);
            // Async save to database or file
            if (plugin.getDatabaseManager().isMySQL()) {
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try (java.sql.Connection conn = plugin.getDatabaseManager().getConnection();
                            java.sql.PreparedStatement ps = conn.prepareStatement(
                                    "REPLACE INTO sg_identities (username, uuid) VALUES (?, ?)")) {
                        ps.setString(1, name);
                        ps.setString(2, uuid.toString());
                        ps.executeUpdate();
                    } catch (java.sql.SQLException e) {
                        plugin.getLogManager().logError("Failed to save identity to MySQL: " + e.getMessage());
                    }
                });
            } else {
                saveIdentities();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBookEdit(PlayerEditBookEvent event) {
        if (!enabled)
            return;

        BookMeta meta = event.getNewBookMeta();

        // Anti-Book Exploit: Malicious users use thousands of characters to lag/crash
        // clients
        if (meta.getPages().size() > 50) {
            event.setCancelled(true);
            event.getPlayer()
                    .sendMessage(ColorUtil.translate("&c[SoulGuard] Book discarded: too many pages (max 50)."));
            return;
        }

        for (String page : meta.getPages()) {
            if (page.length() > 500) {
                event.setCancelled(true);
                event.getPlayer()
                        .sendMessage(ColorUtil.translate("&c[SoulGuard] Book discarded: page content too long."));
                return;
            }
        }
    }
}
