package com.soulguard.config;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.soulguard.SoulGuard;
import com.soulguard.util.ColorUtil;

/**
 * Enhanced Language Manager with Dynamic i18n support.
 * Automatically detects player client-side locale for personalized messages.
 */
public final class LanguageManager {

    private final SoulGuard plugin;
    private final Map<String, FileConfiguration> langCache = new ConcurrentHashMap<>();
    private String defaultLang;

    public LanguageManager(SoulGuard plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String langStr = plugin.getConfig().getString("general.language", "en");
        this.defaultLang = (langStr != null ? langStr : "en").toLowerCase();
        langCache.clear();
        loadLanguage(defaultLang);
    }

    private FileConfiguration loadLanguage(String langCode) {
        if (langCache.containsKey(langCode)) return langCache.get(langCode);

        String fileName = "lang/messages_" + langCode + ".yml";
        File messagesFile = new File(plugin.getDataFolder(), fileName);

        if (!messagesFile.exists()) {
            try {
                plugin.saveResource(fileName, false);
            } catch (Exception e) {
                // Fallback to default if locale file doesn't exist internally
                if (!langCode.equals(defaultLang)) return loadLanguage(defaultLang);
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Load defaults from internal resource if available
        try (InputStream defaultStream = plugin.getResource(fileName)) {
            if (defaultStream != null) {
                try (Reader reader = new InputStreamReader(defaultStream, StandardCharsets.UTF_8)) {
                    YamlConfiguration defaultMessages = new YamlConfiguration();
                    defaultMessages.load(reader);
                    config.setDefaults(defaultMessages);
                }
            }
        } catch (Exception ignored) {}

        langCache.put(langCode, config);
        return config;
    }

    public String getMessage(String path) {
        return getMessageForLocale(defaultLang, path);
    }

    public String getMessage(Player player, String path) {
        if (player == null) return getMessage(path);
        String locale = player.getLocale().split("_")[0].toLowerCase();
        return getMessageForLocale(locale, path);
    }

    private String getMessageForLocale(String locale, String path) {
        FileConfiguration config = loadLanguage(locale);
        String msg = config.getString(path);
        
        if (msg == null && !locale.equals(defaultLang)) {
            // Recursive fallback to default language
            return getMessageForLocale(defaultLang, path);
        }
        
        if (msg == null) return "§c[Missing: " + path + "]";
        return ColorUtil.translate(msg);
    }

    public String getMessage(String path, String placeholder, String value) {
        return getMessage(path).replace(placeholder, value);
    }

    public String getMessage(Player player, String path, String placeholder, String value) {
        return getMessage(player, path).replace(placeholder, value);
    }
}
