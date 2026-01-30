package com.soulguard.modules.chat;

import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;

public class ChatFilter implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;

    private static final java.util.regex.Pattern LINK_PATTERN = 
            java.util.regex.Pattern.compile("(https?://|www\\.|[a-zA-Z0-9-]+\\.[a-zA-Z]{2,3})");

    // Anti-Flood
    private boolean floodEnabled;
    private long floodInterval; // ms
    private final Map<UUID, Long> lastChatTime;

    // Anti-Repetition
    private boolean repetitionEnabled;
    private final Map<UUID, String> lastMessage;

    // Anti-Caps
    private boolean capsEnabled;
    private int capsPercentage;
    private int capsMinLength;

    // Link Filter
    private boolean linkFilterEnabled;

    public ChatFilter(SoulGuard plugin) {
        this.plugin = plugin;
        // Use WeakHashMap for automatic cleanup when players disconnect
        this.lastChatTime = new java.util.WeakHashMap<>();
        this.lastMessage = new java.util.WeakHashMap<>();
    }

    @Override
    public String getName() {
        return "ChatFilter";
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
        this.lastChatTime.clear();
        this.lastMessage.clear();
    }

    @Override
    public void reload() {
        this.floodEnabled = plugin.getConfig().getBoolean("modules.ChatFilter.anti-flood.enabled", true);
        this.floodInterval = plugin.getConfig().getInt("modules.ChatFilter.anti-flood.interval-seconds", 2) * 1000L;

        this.repetitionEnabled = plugin.getConfig().getBoolean("modules.ChatFilter.anti-repetition.enabled", true);

        this.capsEnabled = plugin.getConfig().getBoolean("modules.ChatFilter.anti-caps.enabled", true);
        this.capsPercentage = plugin.getConfig().getInt("modules.ChatFilter.anti-caps.percentage", 70);
        this.capsMinLength = plugin.getConfig().getInt("modules.ChatFilter.anti-caps.min-length", 5);

        this.linkFilterEnabled = plugin.getConfig().getBoolean("modules.ChatFilter.link-filter.enabled", true);

        this.lastChatTime.clear();
        this.lastMessage.clear();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!enabled)
            return;
        Player player = event.getPlayer();
        if (player.hasPermission("soulguard.bypass.chat"))
            return;

        String message = event.getMessage();
        UUID uuid = player.getUniqueId();

        // 1. Anti-Flood
        if (floodEnabled) {
            long lastTime = lastChatTime.getOrDefault(uuid, 0L);
            long now = System.currentTimeMillis();
            if (now - lastTime < floodInterval) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Please do not spam! Wait a few seconds.");
                plugin.getLogManager().logWarn("Player " + player.getName() + " triggered Anti-Flood.");
                if (plugin.getDiscordWebhook() != null) {
                    plugin.getDiscordWebhook().sendAlert("Chat Spam Detected",
                            "Player **" + player.getName() + "** triggered Anti-Flood.", 16755200);
                }
                return;
            }
            lastChatTime.put(uuid, now);
        }

        // 2. Anti-Repetition
        if (repetitionEnabled) {
            String lastMsg = lastMessage.getOrDefault(uuid, "");
            if (message.equalsIgnoreCase(lastMsg)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Do not repeat the same message.");
                plugin.getLogManager().logWarn("Player " + player.getName() + " triggered Anti-Repetition.");
                if (plugin.getDiscordWebhook() != null) {
                    plugin.getDiscordWebhook().sendAlert("Chat Spam Detected",
                            "Player **" + player.getName() + "** triggered Anti-Repetition: `" + message + "`",
                            16755200);
                }
                return;
            }
            lastMessage.put(uuid, message);
        }

        // 3. Anti-Caps
        if (capsEnabled && message.length() >= capsMinLength) {
            int capsCount = 0;
            for (char c : message.toCharArray()) {
                if (Character.isUpperCase(c)) {
                    capsCount++;
                }
            }
            double percentage = (double) capsCount / message.length() * 100;
            if (percentage >= capsPercentage) {
                event.setMessage(message.toLowerCase());
            }
        }

        // 4. Link Filter
        if (linkFilterEnabled && LINK_PATTERN.matcher(message).find()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Links are not allowed in chat.");
            plugin.getLogManager().logWarn("Player " + player.getName() + " tried to send a link: " + message);
            if (plugin.getDiscordWebhook() != null) {
                plugin.getDiscordWebhook().sendAlert("Link Blocked",
                        "Player **" + player.getName() + "** tried to send a link: `" + message + "`", 16755200);
            }
        }
    }
}
