package com.soulguard.modules.anticheat;

import java.util.List;

import org.bukkit.entity.Player;

import com.soulguard.SoulGuard;
import com.soulguard.util.ColorUtil;

public abstract class Check {

    protected final SoulGuard plugin;
    private final String name;
    private final String category;
    private boolean enabled;
    private int violationsToKick;
    private int violationsToSnapshot;
    private List<String> commands;

    public Check(SoulGuard plugin, String name, String category) {
        this.plugin = plugin;
        this.name = name;
        this.category = category;
        loadData();
    }

    public void reload() {
        loadData();
    }

    private void loadData() {
        String path = "modules.anticheat.checks." + category + "." + name;
        int globalIntensity = plugin.getConfig().getInt("modules.anticheat.intensity", 5);
        double multiplier = 1.0 + ((5 - globalIntensity) * 0.1); // Lower intensity = Higher thresholds

        this.enabled = plugin.getConfig().getBoolean(path + ".enabled", true);
        this.violationsToKick = (int) (plugin.getConfig().getInt(path + ".violations.kick", 15) * multiplier);
        this.violationsToSnapshot = (int) (plugin.getConfig().getInt(path + ".violations.snapshot", 5) * multiplier);
        this.commands = plugin.getConfig().getStringList(path + ".violations.commands");
    }

    protected void flag(Player player, String info, double reliability) {
        if (!enabled)
            return;

        CheckManager manager = (CheckManager) plugin.getModuleManager().getModule("NeoGuard");
        if (manager == null) return;
        
        // Context-Aware Sensitivity (TPS Check)
        double tps = manager.getLastTPS();
        if (tps < 18.0) {
            reliability *= (tps / 20.0); // Reduce reliability during lag
            if (reliability < 0.3) return; // Ignore low reliability flags during lag
        }

        PlayerData data = manager.getPlayerData(player.getUniqueId());
        if (data == null)
            return;

        int vl = data.incrementVL(this);
        String errorCode = "SEC-AC-" + (category.substring(0, 1).toUpperCase()) + (name.hashCode() & 0xFFF);

        // Notify staff with Clickable Action Buttons
        String platform = com.soulguard.util.BedrockManager.getPlatform(player);
        int ping = player.getPing();
        
        String alertMsg = ColorUtil.translate("&c&lSoulGuard &8| &e" + errorCode + " &8| &f" + player.getName() + 
                " &8[&7" + platform + "&8] &7failed &e" + name + " &8(&fx" + vl + "&8) &7Ping: &b" + ping + "ms");

        if (plugin.getEdition().isPremium()) {
            try {
                net.md_5.bungee.api.chat.TextComponent base = new net.md_5.bungee.api.chat.TextComponent(alertMsg);
                net.md_5.bungee.api.chat.TextComponent actions = new net.md_5.bungee.api.chat.TextComponent(" ");

                // TP Button
                net.md_5.bungee.api.chat.TextComponent tp = new net.md_5.bungee.api.chat.TextComponent("§b[TP]");
                tp.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                        net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/tp " + player.getName()));
                
                // BAN Button
                net.md_5.bungee.api.chat.TextComponent ban = new net.md_5.bungee.api.chat.TextComponent(" §c[BAN]");
                ban.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                        net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND,
                        "/ban " + player.getName() + " [" + errorCode + "] Cheating"));

                base.addExtra(actions);
                base.addExtra(tp);
                base.addExtra(ban);

                for (Player staff : player.getServer().getOnlinePlayers()) {
                    if (staff.hasPermission("soulguard.anticheat.alerts")) {
                        staff.spigot().sendMessage(base);
                    }
                }
            } catch (NoClassDefFoundError | Exception e) {
                notifyStaff(alertMsg, player);
            }
        } else {
            notifyStaff(alertMsg, player);
        }

        plugin.getLogManager()
                .logWarn("[AC] " + player.getName() + " [" + errorCode + "] failed " + name + " (VL: " + vl + ") Info: " + info);

        if (vl >= violationsToSnapshot && plugin.getInventorySnapshots().isEnabled()) {
            plugin.getInventorySnapshots().takeSnapshot(player, "AC Flag [" + errorCode + "]: " + name);
        }

        if (vl >= violationsToKick) {
            player.kickPlayer(ColorUtil.translate("&c&l[SoulGuard] &8(&e" + errorCode + "&8) &7Seguridad detectó actividad inusual."));
        }

        // Custom commands
        for (String cmd : commands) {
            player.getServer().dispatchCommand(player.getServer().getConsoleSender(),
                    cmd.replace("%player%", player.getName()).replace("%vl%", String.valueOf(vl)).replace("%code%", errorCode));
        }
    }

    private void notifyStaff(String message, Player subject) {
        for (Player staff : subject.getServer().getOnlinePlayers()) {
            if (staff.hasPermission("soulguard.anticheat.alerts")) {
                staff.sendMessage(ColorUtil.translate(message));
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Centralized movement hook to avoid multiple listeners.
     */
    public void handleMove(org.bukkit.event.player.PlayerMoveEvent event, PlayerData data) {
        // Implementation in subclasses
    }
}
