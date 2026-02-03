package com.skguard.logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import com.skguard.api.SKGuardCore;

public class LogManager {

    private final SKGuardCore plugin;
    private final ExecutorService asyncExecutor;
    private File logFile;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public LogManager(SKGuardCore plugin) {
        this.plugin = plugin;
        this.asyncExecutor = Executors.newSingleThreadExecutor();
        setupLogFile();
    }

    private void setupLogFile() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File logsDir = new File(dataFolder, "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        String dateStr = DATE_FORMAT.format(new Date());
        this.logFile = new File(logsDir, "SKGuard-" + dateStr + ".log");
        if (!this.logFile.exists()) {
            try {
                this.logFile.createNewFile();
            } catch (IOException e) {
                internalError("Could not create log file!", e);
            }
        }
    }

    private void internalError(String message, Throwable e) {
        if (plugin.getBukkitLogger() != null) {
            plugin.getBukkitLogger().log(Level.SEVERE, message, e);
        } else if (plugin.getSlf4jLogger() != null) {
            plugin.getSlf4jLogger().error(message, e);
        }
    }

    public void logInfo(String message) {
        log("INFO", message);
        com.skguard.util.ConsoleUtil.info(message);
    }

    public void logWarn(String message) {
        log("WARN", message);
        com.skguard.util.ConsoleUtil.warn(message);
    }

    public void logError(String message) {
        log("ERROR", message);
        com.skguard.util.ConsoleUtil.error(message);
    }

    private void log(String level, String message) {
        asyncExecutor.submit(() -> {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                String timestamp = TIME_FORMAT.format(new Date());
                writer.write(String.format("[%s] [%s] %s", timestamp, level, message));
                writer.newLine();
            } catch (IOException e) {
                internalError("Internal logging error", e);
            }
        });
    }

    public void shutdown() {
        asyncExecutor.shutdown();
    }
}

