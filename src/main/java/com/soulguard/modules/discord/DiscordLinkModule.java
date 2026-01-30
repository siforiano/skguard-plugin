package com.soulguard.modules.discord;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import com.soulguard.util.ColorUtil;

public class DiscordLinkModule implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    private final Map<UUID, String> pendingPin = new HashMap<>();
    // private final Map<UUID, String> linkedAccounts = new HashMap<>();

    public DiscordLinkModule(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "DiscordVerify";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = plugin.getConfig().getBoolean("modules.DiscordVerify.enabled", false);
    }

    @Override
    public void disable() {
        this.enabled = false;
    }

    @Override
    public void reload() {
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        // Step 3 (Discord 2FA) is now triggered exclusively by Step 2 (StaffPIN)
        // to ensure linear authentication flow (Login -> PIN -> 2FA).
    }

    public void triggerVerification(Player player) {
        String code = String.format("%04d", new Random().nextInt(10000));
        pendingPin.put(player.getUniqueId(), code);

        player.sendMessage(
                ColorUtil.translate("&9&l[Discord] &7A verification code has been sent to your linked Discord."));
        player.sendMessage(ColorUtil.translate("&7Please type the code in chat to continue."));

        // Simulate sending to Discord
        if (plugin.getDiscordWebhook().isEnabled()) {
            plugin.getDiscordWebhook().sendSimpleEmbed("Staff Verification Required",
                    "**Staff:** " + player.getName() + " is trying to login.\n**Verification Code:** `" + code + "`",
                    "5865F2");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!enabled)
            return;

        Player player = event.getPlayer();
        if (pendingPin.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            String input = event.getMessage().trim();
            String code = pendingPin.get(player.getUniqueId());

            if (input.equals(code)) {
                pendingPin.remove(player.getUniqueId());
                player.sendMessage(ColorUtil.translate("&a&l[Discord] &fVerification successful! Welcome back."));
                plugin.getAuditManager().logAction(player, "DISCORD_VERIFY_SUCCESS", "Correct code entered.");

                // Release from quarantine if PIN is also verified
                if (!plugin.getStaffPIN().isEnabled() || !plugin.getStaffPIN().isPending(player.getUniqueId())) {
                    plugin.getQuarantineManager().releasePlayer(player);
                    // Create Global Session (Network Mode)
                    if (player.getAddress() != null) {
                        String ip = player.getAddress().getAddress().getHostAddress();
                        plugin.getSessionManager().createSession(player.getUniqueId(), ip, 3600000); // 1 hour session
                    }
                }
            } else {
                player.sendMessage(ColorUtil.translate("&c&l[Discord] &7Incorrect code. Please check your Discord."));
            }
        }
    }

    public boolean isVerified(UUID uuid) {
        return !pendingPin.containsKey(uuid);
    }
}
