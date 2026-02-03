package com.skguard.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.skguard.api.SKGuardCore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseManager {

    private final SKGuardCore plugin;
    private HikariDataSource dataSource;
    private boolean useMySQL;

    public DatabaseManager(SKGuardCore plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        this.useMySQL = plugin.getConfig().getBoolean("database.mysql.enabled", false);

        if (useMySQL) {
            setupMySQL();
        } else {
            plugin.getLogManager().logInfo("Using FlatFile (YAML) for data storage.");
        }
    }

    private void setupMySQL() {
        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("database.mysql.port", 3306);
        String database = plugin.getConfig().getString("database.mysql.database", "SKGuard");
        String username = plugin.getConfig().getString("database.mysql.username", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "");
        boolean useSSL = plugin.getConfig().getBoolean("database.mysql.ssl", false);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        try {
            this.dataSource = new HikariDataSource(config);
            createTables();
            plugin.getLogManager().logInfo("Successfully connected to MySQL database.");
        } catch (Exception e) {
            plugin.getLogManager().logError("Could not connect to MySQL database! Falling back to YAML: " + e.getMessage());
            this.useMySQL = false;
        }
    }

    private void createTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Users table (Auth)
            stmt.execute("CREATE TABLE IF NOT EXISTS sg_users (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "username VARCHAR(16), " +
                    "password VARCHAR(255), " +
                    "ip VARCHAR(45), " +
                    "reg_date LONG)");

            // PINs table
            stmt.execute("CREATE TABLE IF NOT EXISTS sg_pins (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "pin VARCHAR(255))");

            // Identities table (IdentityGuard persistence)
            stmt.execute("CREATE TABLE IF NOT EXISTS sg_identities (" +
                    "username VARCHAR(16) PRIMARY KEY, " +
                    "uuid VARCHAR(36))");

            // Sessions table (Global Network Auth)
            stmt.execute("CREATE TABLE IF NOT EXISTS sg_sessions (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "ip VARCHAR(45), " +
                    "expiry BIGINT)");

            plugin.getLogManager().logInfo("Database tables verified/created.");
        } catch (SQLException e) {
            plugin.getLogManager().logError("Could not create database tables: " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || !useMySQL)
            return null;
        return dataSource.getConnection();
    }

    public boolean isMySQL() {
        return useMySQL;
    }

    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}

