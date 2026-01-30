package com.soulguard.logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;
import com.soulguard.util.ColorUtil;

public class AuditManager implements SecurityModule {

    private final SoulGuard plugin;
    private boolean enabled;
    private final java.util.Map<java.util.UUID, java.util.List<String>> encryptedHistory = new java.util.concurrent.ConcurrentHashMap<>();

    public AuditManager(SoulGuard plugin) {
        this.plugin = plugin;
    }

    public void trackCommand(java.util.UUID uuid, String command) {
        if (!enabled) return;
        
        String masterKey = plugin.getConfig().getString("general.master-key", "default_soulguard_key");
        String encrypted = com.soulguard.util.EncryptionUtil.encrypt(command, masterKey);
        
        encryptedHistory.computeIfAbsent(uuid, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(encrypted);
        
        // Limit history size per session
        java.util.List<String> history = encryptedHistory.get(uuid);
        if (history.size() > 100) {
            history.remove(0);
        }
    }

    public void clearHistory(java.util.UUID uuid) {
        encryptedHistory.remove(uuid);
    }

    public java.util.List<String> getDecryptedHistory(java.util.UUID uuid, String masterKey) {
        java.util.List<String> raw = encryptedHistory.get(uuid);
        if (raw == null) return java.util.Collections.emptyList();
        
        java.util.List<String> decrypted = new java.util.ArrayList<>();
        for (String encrypted : raw) {
            String plain = com.soulguard.util.EncryptionUtil.decrypt(encrypted, masterKey);
            if (plain != null) decrypted.add(plain);
        }
        return decrypted;
    }

    @Override
    public String getName() {
        return "AuditManager";
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

    public void searchLogs(Player player, String query, int limit) {
        // --- Privacy Enforcement (SEC-AUD-001) ---
        boolean strictPrivacy = plugin.getConfig().getBoolean("general.privacy.strict-audit", true);
        boolean isFullAccess = player.hasPermission("soulguard.audit.full") || player.isOp();
        
        if (strictPrivacy && !isFullAccess) {
             // Restriction: search query must match their own name/uuid
             if (!query.equalsIgnoreCase(player.getName()) && !query.equalsIgnoreCase(player.getUniqueId().toString())) {
                 player.sendMessage(ColorUtil.translate("&c&l[Security] &7Privacy Policy: Access to external logs denied. &8(SEC-AUD-001)"));
                 return;
             }
        }

        player.sendMessage(
                ChatColor.YELLOW + "[SoulGuard] Searching for: " + ChatColor.WHITE + query + " (Asynchronous)...");

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            File logDir = new File(plugin.getDataFolder(), "logs");
            if (!logDir.exists()) {
                player.sendMessage(ChatColor.RED + "No logs found.");
                return;
            }

            File[] files = logDir.listFiles((dir, name) -> name.endsWith(".log"));
            if (files == null || files.length == 0) {
                player.sendMessage(ChatColor.RED + "No log files found.");
                return;
            }

            // Sort by last modified (latest first)
            Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

            List<String> results = new ArrayList<>();

            for (File file : files) {
                if (results.size() >= limit)
                    break;

                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String lowerLine = line.toLowerCase();
                        if (lowerLine.contains(query.toLowerCase())) {
                            // Mask IP addresses (IPv4 and IPv6) if no full access
                            if (!isFullAccess) {
                                line = line.replaceAll("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})|([0-9a-fA-F]{1,4}(:[0-9a-fA-F]{1,4}){7})", "XXX.XXX.XXX.XXX");
                            }
                            results.add(line);
                            if (results.size() >= limit)
                                break;
                        }
                    }
                } catch (IOException e) {
                    plugin.getLogManager().logError("Error reading log file: " + file.getName());
                }
            }

            // Return results to the player on the main thread (optional, but safer for
            // UI/Message consistency)
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (results.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "No results found for '" + query + "'.");
                } else {
                    for (String result : results) {
                        player.sendMessage(formatLogLine(result));
                    }
                    player.sendMessage(ChatColor.GRAY + "Showing " + results.size() + " matches asynchronously.");
                }
            });
        });
    }

    private String formatLogLine(String line) {
        if (line.contains("INFO"))
            return ChatColor.GRAY + line;
        if (line.contains("WARN"))
            return ChatColor.YELLOW + line;
        if (line.contains("ERROR"))
            return ChatColor.RED + line;
        return line;
    }

    public void logAction(Player player, String action, String details) {
        plugin.getLogManager().logInfo("[AUDIT] " + player.getName() + " | " + action + " | " + details);
    }

    public List<String> getRecentLogs(int limit) {
        List<String> logs = new ArrayList<>();
        File logDir = new File(plugin.getDataFolder(), "logs");
        if (!logDir.exists())
            return logs;

        File[] files = logDir.listFiles((dir, name) -> name.endsWith(".log"));
        if (files == null || files.length == 0)
            return logs;

        Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        for (File file : files) {
            // OPTIMIZATION: Read from end using RandomAccessFile to avoid OOM
            try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "r")) {
                long fileLength = file.length();
                long pointer = fileLength - 1;
                StringBuilder sb = new StringBuilder();

                while (pointer >= 0 && logs.size() < limit) {
                    raf.seek(pointer);
                    int c = raf.read();
                    if (c == '\n') {
                        if (sb.length() > 0) {
                            logs.add(sb.reverse().toString());
                            sb.setLength(0);
                            if (logs.size() >= limit)
                                break;
                        }
                    } else if (c != '\r') {
                        sb.append((char) c);
                    }
                    pointer--;
                }
                // Last partial line
                if (sb.length() > 0 && logs.size() < limit) {
                    logs.add(sb.reverse().toString());
                }
            } catch (IOException ignored) {
            }

            if (logs.size() >= limit)
                break;
        }
        return logs;
    }
}
