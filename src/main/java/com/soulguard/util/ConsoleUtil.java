package com.soulguard.util;

import org.bukkit.Bukkit;

public class ConsoleUtil {

    private static final String PREFIX = "§8[§bSoulGuard§8] §f";
    
    public static void sendBanner() {
        org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("SoulGuard");
        String version = (plugin != null) ? plugin.getDescription().getVersion() : "1.0";
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
            Bukkit.getConsoleSender().sendMessage(line);
        }
    }

    public static void info(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + "§7INFO: §f" + message);
    }

    public static void success(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + "§aSUCCESS: §f" + message);
    }

    public static void warn(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + "§eWARN: §6" + message);
    }

    public static void error(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + "§4ERROR: §c" + message);
    }

    public static void moduleLoad(String moduleName, boolean enabled) {
        String status = enabled ? "§a§lENABLED" : "§c§lDISABLED";
        Bukkit.getConsoleSender().sendMessage("§8  » §fModule: §b" + String.format("%-20s", moduleName) + " §7[" + status + "§7]");
    }
    
    public static void phase(String phase) {
        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("§b§l> §f" + phase.toUpperCase());
    }
}
