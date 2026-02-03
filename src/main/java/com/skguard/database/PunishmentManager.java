package com.skguard.database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.skguard.api.SKGuardCore;
 
public class PunishmentManager {

    private final SKGuardCore plugin;
    private File file;
    private FileConfiguration config;
    
    // CACHE: Zero-latency lookup
    private final Map<UUID, List<Punishment>> punishmentCache = new ConcurrentHashMap<>();

    public PunishmentManager(SKGuardCore plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        if (!plugin.getDatabaseManager().isMySQL()) {
            file = new File(plugin.getDataFolder(), "punishments.yml");
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException ignored) {}
            }
            config = YamlConfiguration.loadConfiguration(file);
        } else {
            createTable();
        }
    }

    private void createTable() {
        try (Connection conn = plugin.getDatabaseManager().getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS sg_punishments (" +
                    "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                    "uuid VARCHAR(36), " +
                    "ip VARCHAR(45), " +
                    "type VARCHAR(10), " +
                    "reason TEXT, " +
                    "operator VARCHAR(16), " +
                    "issued_at LONG, " +
                    "expiry LONG, " +
                    "active BOOLEAN)");
        } catch (SQLException e) {
            plugin.getLogManager().logError("Could not create punishments table: " + e.getMessage());
        }
    }

    /**
     * Loads punishments from DB into cache. Blocking method (Call in AsyncPlayerPreLoginEvent).
     */
    public void loadCache(UUID uuid) {
        try {
            List<Punishment> history = fetchHistorySync(uuid);
            punishmentCache.put(uuid, history);
        } catch (Exception e) {
            plugin.getLogManager().logError("Failed to load punishment cache for " + uuid + ": " + e.getMessage());
            punishmentCache.put(uuid, new ArrayList<>()); // Put empty list to avoid NPE
        }
    }
    
    public void unloadCache(UUID uuid) {
        punishmentCache.remove(uuid);
    }

    public CompletableFuture<Void> addPunishment(Punishment p) {
        // Update cache immediately
        punishmentCache.computeIfAbsent(p.uuid, k -> new ArrayList<>()).add(0, p);
        
        return CompletableFuture.runAsync(() -> {
            if (plugin.getDatabaseManager().isMySQL()) {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "INSERT INTO sg_punishments (uuid, ip, type, reason, operator, issued_at, expiry, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setString(1, p.uuid != null ? p.uuid.toString() : null);
                    ps.setString(2, p.ip);
                    ps.setString(3, p.type.name());
                    ps.setString(4, p.reason);
                    ps.setString(5, p.operator);
                    ps.setLong(6, p.issuedAt);
                    ps.setLong(7, p.expiry);
                    ps.setBoolean(8, p.active);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogManager().logError("Error saving punishment: " + e.getMessage());
                }
            } else {
                synchronized (this) {
                    String path = p.uuid.toString() + "." + System.currentTimeMillis();
                    config.set(path + ".type", p.type.name());
                    config.set(path + ".reason", p.reason);
                    config.set(path + ".operator", p.operator);
                    config.set(path + ".issued_at", p.issuedAt);
                    config.set(path + ".expiry", p.expiry);
                    config.set(path + ".active", p.active);
                    saveConfig();
                }
            }
        });
    }

    // Direct DB fetch (Helper for loadCache)
    private List<Punishment> fetchHistorySync(UUID uuid) {
        List<Punishment> history = new ArrayList<>();
        if (plugin.getDatabaseManager().isMySQL()) {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("SELECT * FROM sg_punishments WHERE uuid = ? ORDER BY issued_at DESC")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        history.add(new Punishment(
                                rs.getString("uuid") != null ? UUID.fromString(rs.getString("uuid")) : null,
                                rs.getString("ip"),
                                PunishmentType.valueOf(rs.getString("type")),
                                rs.getString("reason"),
                                rs.getString("operator"),
                                rs.getLong("issued_at"),
                                rs.getLong("expiry"),
                                rs.getBoolean("active")
                        ));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogManager().logError("Error loading history: " + e.getMessage());
            }
        } else {
            synchronized (this) {
                if (config.contains(uuid.toString())) {
                    org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection(uuid.toString());
                    if (section != null) {
                        for (String key : section.getKeys(false)) {
                            String path = uuid.toString() + "." + key;
                            history.add(new Punishment(
                                    uuid,
                                    null,
                                    PunishmentType.valueOf(config.getString(path + ".type")),
                                    config.getString(path + ".reason"),
                                    config.getString(path + ".operator"),
                                    config.getLong(path + ".issued_at"),
                                    config.getLong(path + ".expiry"),
                                    config.getBoolean(path + ".active")
                            ));
                        }
                    }
                }
            }
        }
        return history;
    }

    public CompletableFuture<List<Punishment>> getHistory(UUID uuid) {
        // Return cache if available, otherwise fetch async
        if (punishmentCache.containsKey(uuid)) {
            return CompletableFuture.completedFuture(new ArrayList<>(punishmentCache.get(uuid)));
        }
        return CompletableFuture.supplyAsync(() -> fetchHistorySync(uuid));
    }
    
    public Punishment getActivePunishment(UUID uuid, PunishmentType type) {
        List<Punishment> history = punishmentCache.get(uuid);
        if (history == null) {
            history = fetchHistorySync(uuid); 
        }

        return history.stream()
                .filter(p -> p.type == type && p.active)
                .filter(p -> p.expiry <= 0 || System.currentTimeMillis() <= p.expiry)
                .findFirst()
                .orElse(null);
    }

    public Punishment getActiveIPPunishment(String ip, PunishmentType type) {
        if (plugin.getDatabaseManager().isMySQL()) {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT * FROM sg_punishments WHERE ip = ? AND type = ? AND active = true ORDER BY issued_at DESC")) {
                ps.setString(1, ip);
                ps.setString(2, type.name());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        long expiry = rs.getLong("expiry");
                        if (expiry <= 0 || System.currentTimeMillis() <= expiry) {
                            return new Punishment(
                                    rs.getString("uuid") != null ? UUID.fromString(rs.getString("uuid")) : null,
                                    rs.getString("ip"),
                                    PunishmentType.valueOf(rs.getString("type")),
                                    rs.getString("reason"),
                                    rs.getString("operator"),
                                    rs.getLong("issued_at"),
                                    rs.getLong("expiry"),
                                    rs.getBoolean("active")
                            );
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogManager().logError("Error checking IP punishment: " + e.getMessage());
            }
        }
        return null;
    }

    public CompletableFuture<Void> deactivatePunishment(UUID uuid, PunishmentType type) {
        // Update Cache
        if (punishmentCache.containsKey(uuid)) {
            punishmentCache.get(uuid).stream()
                .filter(p -> p.type == type && p.active)
                .forEach(p -> p.active = false);
        }

        return CompletableFuture.runAsync(() -> {
            if (plugin.getDatabaseManager().isMySQL()) {
                try (Connection conn = plugin.getDatabaseManager().getConnection();
                     PreparedStatement ps = conn.prepareStatement(
                             "UPDATE sg_punishments SET active = false WHERE uuid = ? AND type = ? AND active = true")) {
                    ps.setString(1, uuid.toString());
                    ps.setString(2, type.name());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogManager().logError("Error deactivating punishment: " + e.getMessage());
                }
            } else {
                synchronized (this) {
                    if (config.contains(uuid.toString())) {
                        org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection(uuid.toString());
                        if (section != null) {
                            for (String key : section.getKeys(false)) {
                                String path = uuid.toString() + "." + key;
                                String typeStr = config.getString(path + ".type");
                                if (type.name().equals(typeStr)) {
                                    config.set(path + ".active", false);
                                }
                            }
                        }
                        saveConfig();
                    }
                }
            }
        });
    }

    private void saveConfig() {
        try {
            config.save(file);
        } catch (IOException ignored) {}
    }

    public enum PunishmentType {
        BAN, MUTE, WARN
    }

    public static class Punishment {
        public final UUID uuid;
        public final String ip;
        public final PunishmentType type;
        public final String reason;
        public final String operator;
        public final long issuedAt;
        public final long expiry; // 0 for permanent
        public boolean active;

        public Punishment(UUID uuid, String ip, PunishmentType type, String reason, String operator, long issuedAt, long expiry, boolean active) {
            this.uuid = uuid;
            this.ip = ip;
            this.type = type;
            this.reason = reason;
            this.operator = operator;
            this.issuedAt = issuedAt;
            this.expiry = expiry;
            this.active = active;
        }
    }
}

