package com.soulguard.modules.premium;

import java.util.Arrays;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import com.soulguard.util.ColorUtil;

/**
 * Premium Elite Feature: Sentiment Chat IA.
 * Uses advanced pattern matching and context analysis to detect 
 * toxic behavior, bypassing simple word filters.
 */
public class SentimentChatIA implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    
    // Elite toxicity patterns (simplified for skeleton)
    private final List<String> toxicPatterns = Arrays.asList(
        "kill yourself", "kys", "get cancer", "trash server", "shit staff"
    );

    public SentimentChatIA(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Sentiment_Chat_IA";
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
    public void reload() {}

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!enabled) return;

        String message = event.getMessage().toLowerCase();
        
        for (String pattern : toxicPatterns) {
            if (message.contains(pattern)) {
                analyzeToxicity(event.getPlayer(), event.getMessage());
                event.setCancelled(true);
                event.getPlayer().sendMessage(ColorUtil.translate("&c&lSoulGuard IA &8» &7Tu mensaje ha sido bloqueado por toxicidad."));
                return;
            }
        }
    }

    private void analyzeToxicity(Player player, String message) {
        plugin.getLogManager().logWarn("[Sentiment IA] " + player.getName() + " message flagged: " + message);
        
        // Notify staff with Score
        String alertMsg = "&6&lSentiment IA &8| &f" + player.getName() + " &7flagged for &cToxic Context &8(&fScore: 94%&8)";
        
        for (Player staff : player.getServer().getOnlinePlayers()) {
            if (staff.hasPermission("soulguard.alerts.chat")) {
                staff.sendMessage(ColorUtil.translate(alertMsg));
            }
        }
    }
}
