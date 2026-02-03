package com.skguard.modules.panic;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RollbackManager implements SecurityModule, Listener {

    private final SKGuard plugin;
    private boolean enabled;
    private final Map<UUID, Deque<StaffAction>> actionHistory = new ConcurrentHashMap<>();

    public RollbackManager(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "RollbackManager";
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
        actionHistory.clear();
    }

    @Override
    public void reload() {
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStaffCommand(PlayerCommandPreprocessEvent event) {
        if (!enabled)
            return;
        Player player = event.getPlayer();

        if (player.hasPermission("SKGuard.staff")) {
            StaffAction action = new StaffAction(event.getMessage(), System.currentTimeMillis());
            actionHistory.computeIfAbsent(player.getUniqueId(), k -> new ArrayDeque<>()).addFirst(action);

            // Limit history to 50 actions
            if (actionHistory.get(player.getUniqueId()).size() > 50) {
                actionHistory.get(player.getUniqueId()).removeLast();
            }
        }
    }

    public void rollbackRecentActions(UUID staffUuid) {
        Deque<StaffAction> history = actionHistory.remove(staffUuid);
        if (history == null || history.isEmpty())
            return;

        plugin.getLogManager().logInfo("Starting emergency rollback for staff " + staffUuid);
        int undone = 0;

        for (StaffAction action : history) {
            String cmd = action.command.toLowerCase();

            // We only rollback destructive commands we can safely invert or log
            if (cmd.startsWith("/ban ")) {
                String target = cmd.split(" ")[1];
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pardon " + target);
                undone++;
            } else if (cmd.startsWith("/op ")) {
                String target = cmd.split(" ")[1];
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "deop " + target);
                undone++;
            }
            // Add more rollback logic as needed
        }

        plugin.getLogManager().logInfo("Rollback completed: " + undone + " actions reverted.");
    }

    private static class StaffAction {
        final String command;

        StaffAction(String command, long time) {
            this.command = command;
        }
    }
}

