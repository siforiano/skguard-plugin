package com.skguard.util;

public class ConsoleUtil {

    private static final String PREFIX_BUKKIT = "§8[§bSKGuard§8] §f";
    private static final String PREFIX_ANSI = "\u001B[38;5;244m[\u001B[38;5;45mSKGuard\u001B[38;5;244m] \u001B[0m";
    
    private static Object logger; // Logger (Velocity) or null (Bukkit)
    private static boolean isVelocity = false;

    public static void initialize(Object platformLogger) {
        logger = platformLogger;
        isVelocity = platformLogger != null;
    }

    public static void sendBanner() {
        String version = "1.0-SNAPSHOT";
        String[] banner = {
            "§b   _____             _  _____                     _ ",
            "§b  / ____|           | |/ ____|                   | |",
            "§b | (___   ___  _   _| | |  __ _   _  __ _ _ __ __| |",
            "§b  \\___ \\ / _ \\| | | | | | |_ | | | |/ _` | '__/ _` |",
            "§b  ____) | (_) | |_| | | |__| | |_| | (_| | | | (_| |",
            "§b |_____/ \\___/ \\__,_|_|\\_____|\\__,_|\\__,_|_|  \\__,_|",
            "§7                                                    ",
            "§f        Advanced Security Framework §7- §b v" + version,
            "§8       Developed with expertise for premium servers    ",
            ""
        };
        for (String line : banner) {
            log(line);
        }
    }

    private static void log(String message) {
        if (isVelocity) {
            String ansiMessage = translateToAnsi(message);
            ((org.slf4j.Logger) logger).info(ansiMessage);
        } else {
            org.bukkit.Bukkit.getConsoleSender().sendMessage(message);
        }
    }

    private static String translateToAnsi(String message) {
        return message
            .replace("§b", "\u001B[38;5;45m")
            .replace("§f", "\u001B[37m")
            .replace("§7", "\u001B[38;5;244m")
            .replace("§8", "\u001B[38;5;239m")
            .replace("§a", "\u001B[32m")
            .replace("§e", "\u001B[33m")
            .replace("§6", "\u001B[38;5;208m")
            .replace("§4", "\u001B[31m")
            .replace("§c", "\u001B[38;5;203m")
            .replace("§l", "\u001B[1m")
            .concat("\u001B[0m");
    }

    public static void info(String message) {
        log(PREFIX_BUKKIT + "§7INFO: §f" + message);
    }

    public static void success(String message) {
        log(PREFIX_BUKKIT + "§aSUCCESS: §f" + message);
    }

    public static void warn(String message) {
        log(PREFIX_BUKKIT + "§eWARN: §6" + message);
    }

    public static void error(String message) {
        log(PREFIX_BUKKIT + "§4ERROR: §c" + message);
    }

    public static void moduleLoad(String moduleName, boolean enabled) {
        String status = enabled ? "§a§lENABLED" : "§c§lDISABLED";
        log("§8  » §fModule: §b" + String.format("%-20s", moduleName) + " §7[" + status + "§7]");
    }
    
    public static void phase(String phase) {
        log("");
        log("§b§l> §f" + phase.toUpperCase());
    }
}

