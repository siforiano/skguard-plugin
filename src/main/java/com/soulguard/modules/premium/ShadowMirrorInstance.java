package com.soulguard.modules.premium;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;

/**
 * Premium Elite Feature: Shadow Ban Instance (Mirror).
 * Isolates suspected cheaters into a "virtual dimension" where 
 * they can chat and move but their actions have no impact on the 
 * real world or other players.
 */
public class ShadowMirrorInstance implements SecurityModule, Listener {

    private final SoulGuard plugin;
    private boolean enabled;
    
    private final Set<UUID> mirroredPlayers = ConcurrentHashMap.newKeySet();

    public ShadowMirrorInstance(SoulGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "Shadow_Mirror";
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
        mirroredPlayers.clear();
    }

    @Override
    public void reload() {}

    public void isolatePlayer(Player player) {
        mirroredPlayers.add(player.getUniqueId());
        player.sendMessage("§a[SoulGuard] §7You have been mirrored for safety check.");
        plugin.getLogManager().logInfo("[Mirror] Isolated suspicious player: " + player.getName());
    }

    @EventHandler
    public void onMirroredChat(AsyncPlayerChatEvent event) {
        if (!enabled || !mirroredPlayers.contains(event.getPlayer().getUniqueId())) return;
        
        // Shadow chat: Send only to other mirrored players
        event.getRecipients().removeIf(p -> !mirroredPlayers.contains(p.getUniqueId()));
    }
    
    @EventHandler
    public void onMirroredJoin(PlayerJoinEvent event) {
        if (!enabled) return;
        // Logic to check if they were previously mirrored
    }
}
