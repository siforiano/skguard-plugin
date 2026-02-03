package com.skguard.modules.chat;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;
import com.skguard.util.ColorUtil;

public class PrivacyGuard implements SecurityModule, Listener {

    private final SKGuard plugin;
    private boolean enabled;

    // RegEx patterns
    private final Pattern phonePattern = Pattern.compile("\\b(\\+?\\d{1,3}[- ]?)?\\d{3}[- ]?\\d{3}[- ]?\\d{4}\\b");
    private final Pattern emailPattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private final Pattern discordTokenPattern = Pattern.compile("[MN][A-Za-z\\d]{23}\\.[\\w-]{6}\\.[\\w-]{27}");

    private final List<String> phishingDomains = Arrays.asList(
            "free-ranks.net", "optifine-capes.xyz", "minecraft-shop.ru", "discord-gift.com");

    private final List<String> selfHarmKeywords = Arrays.asList(
            "suicidio", "matarme", "cortarme", "autolesi", "si no fuera por", "adiós mundo");

    public PrivacyGuard(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "PrivacyGuard";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
    }

    @Override
    public void disable() {
        this.enabled = false;
    }

    @Override
    public void reload() {
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!enabled)
            return;

        String message = event.getMessage();

        // 1. PII Detection (Personal Identifiable Information)
        if (phonePattern.matcher(message).find() ||
                emailPattern.matcher(message).find() ||
                discordTokenPattern.matcher(message).find()) {

            event.setCancelled(true);
            event.getPlayer().sendMessage(
                    ColorUtil.translate("&c&l[Security] &7Message blocked! Avoid sharing personal data in chat."));
            plugin.getLogManager().logInfo("PII blocked from " + event.getPlayer().getName());
            return;
        }

        // 2. Phishing Protection
        for (String domain : phishingDomains) {
            if (message.toLowerCase().contains(domain)) {
                event.setCancelled(true);
                event.getPlayer()
                        .sendMessage(ColorUtil.translate("&c&l[Security] &7Malicious link detected and blocked."));
                plugin.getLogManager()
                        .logWarn("Phishing attempt blocked from " + event.getPlayer().getName() + ": " + domain);
                return;
            }
        }

        // 3. Anti-Self-Harm Detection
        for (String keyword : selfHarmKeywords) {
            if (message.toLowerCase().contains(keyword)) {
                plugin.getLogManager().logWarn("Potential Self-Harm alert for " + event.getPlayer().getName() + ": " + message);
                if (plugin.getDiscordWebhook().isEnabled()) {
                    plugin.getDiscordWebhook().sendAlert("Critical: Self-Harm Alert",
                        "Player **" + event.getPlayer().getName() + "** said keywords: `" + message + "`\n" +
                        "Please check on them immediately.", 16711680);
                }
                break;
            }
        }
    }
}

