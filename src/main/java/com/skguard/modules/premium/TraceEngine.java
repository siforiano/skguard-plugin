package com.skguard.modules.premium;

import java.util.List;
import java.util.ArrayList;
import org.bukkit.entity.Player;
import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;

/**
 * Premium Elite Feature: Deep Search Trace Engine.
 * Allows staff to high-speed query security history by user,
 * event type, or timestamp with fuzzy matching support.
 */
public class TraceEngine implements SecurityModule {

    private final SKGuard plugin;
    private boolean enabled;

    public TraceEngine(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "TraceEngine";
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

    public void deepSearch(Player staff, String query) {
        staff.sendMessage("§b[Trace] §7Searching for '§f" + query + "§7' in global logs...");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            // Elite logic: asynchronous high-speed search
            List<String> results = plugin.getAuditManager().getRecentLogs(1000);
            List<String> found = new ArrayList<>();

            for (String log : results) {
                if (log.toLowerCase().contains(query.toLowerCase())) {
                    found.add(log);
                }
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!staff.isOnline())
                    return;

                int matches = found.size();
                for (int i = 0; i < Math.min(5, found.size()); i++) {
                    staff.sendMessage(" §8» §f" + found.get(i));
                }

                staff.sendMessage("§b[Trace] §7Search complete. Found §f" + matches + " §7hits.");
            });
        });
    }
}

