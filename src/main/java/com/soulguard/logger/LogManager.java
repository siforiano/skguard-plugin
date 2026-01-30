package com.soulguard.logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import com.soulguard.SoulGuard;

public class LogManager {

    private final SoulGuard plugin;
    private final ExecutorService asyncExecutor;
    private File logFile;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public LogManager(SoulGuard plugin) {
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
        this.logFile = new File(logsDir, "soulguard-" + dateStr + ".log");
        if (!this.logFile.exists()) {
            try {
                this.logFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create log file!", e);
            }
        }
    }

    public void logInfo(String message) {
        log("INFO", message);
        com.soulguard.util.ConsoleUtil.info(message);
    }

    public void logWarn(String message) {
        log("WARN", message);
        com.soulguard.util.ConsoleUtil.warn(message);
    }

    public void logError(String message) {
        log("ERROR", message);
        com.soulguard.util.ConsoleUtil.error(message);
    }

    private void log(String level, String message) {
        asyncExecutor.submit(() -> {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                String timestamp = TIME_FORMAT.format(new Date());
                writer.write(String.format("[%s] [%s] %s", timestamp, level, message));
                writer.newLine();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Internal logging error", e);
            }
        });
    }

    public void shutdown() {
        asyncExecutor.shutdown();
    }
}
