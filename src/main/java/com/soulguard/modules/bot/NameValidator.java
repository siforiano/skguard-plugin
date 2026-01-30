package com.soulguard.modules.bot;

import java.util.regex.Pattern;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import com.soulguard.util.ColorUtil;

public class NameValidator implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private Pattern regexPattern;

    public NameValidator(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "NameValidator";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        reload();
    }

    @Override
    public void disable() {
        this.enabled = false;
        this.regexPattern = null;
    }

    @Override
    public void reload() {
        org.bukkit.configuration.ConfigurationSection section = plugin.getConfig().getConfigurationSection("modules.NameValidator");
        String regex = section != null ? section.getString("regex", "[a-zA-Z0-9_]{3,16}") : "[a-zA-Z0-9_]{3,16}";
        try {
            if (regex != null) this.regexPattern = Pattern.compile(regex);
        } catch (Exception e) {
            plugin.getLogManager().logError("Invalid regex for NameValidator: " + regex);
            this.regexPattern = Pattern.compile("[a-zA-Z0-9_]{3,16}");
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAsyncLogin(AsyncPlayerPreLoginEvent event) {
        if (!enabled || regexPattern == null)
            return;

        String name = event.getName();
        if (!regexPattern.matcher(name).matches()) {
            String kickMsg = plugin.getConfig().getString("modules.NameValidator.kick-message", "&cInvalid username.");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ColorUtil.translate(kickMsg != null ? kickMsg : "&cInvalid username."));
            plugin.getLogManager().logWarn("Blocked invalid username: " + name);
            if (plugin.getDiscordWebhook() != null) {
                plugin.getDiscordWebhook().sendAlert("Bot Filter",
                        "Blocked invalid username: **" + name + "**", 16711680);
            }
        }
    }
}
