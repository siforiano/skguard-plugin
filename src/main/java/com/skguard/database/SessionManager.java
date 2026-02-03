package com.skguard.database;

import com.skguard.SKGuard;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SessionManager {

    private final SKGuard plugin;

    public SessionManager(SKGuard plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Boolean> hasActiveSession(UUID uuid, String ip) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT expiry FROM sg_sessions WHERE uuid = ? AND ip = ?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, ip);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        long expiry = rs.getLong("expiry");
                        return expiry > System.currentTimeMillis();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogManager().logError("Failed to check global session: " + e.getMessage());
            }
            return false;
        });
    }

    public void createSession(UUID uuid, String ip, long durationMs) {
        long expiry = System.currentTimeMillis() + durationMs;
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "REPLACE INTO sg_sessions (uuid, ip, expiry) VALUES (?, ?, ?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, ip);
                ps.setLong(3, expiry);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogManager().logError("Failed to create global session: " + e.getMessage());
            }
        });
    }

    public void removeSession(UUID uuid) {
        CompletableFuture.runAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM sg_sessions WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogManager().logError("Failed to remove global session: " + e.getMessage());
            }
        });
    }
}

