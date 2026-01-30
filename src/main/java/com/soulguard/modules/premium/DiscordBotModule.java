package com.soulguard.modules.premium;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.bukkit.entity.Player;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class DiscordBotModule implements SecurityModule {

    private final SoulGuard plugin;
    private boolean enabled;
    private String botToken;
    private String alertChannelId;
    private final Map<String, DiscordCommand> commands;
    private JDA jda;

    public DiscordBotModule(SoulGuard plugin) {
        this.plugin = plugin;
        this.commands = new HashMap<>();
        registerCommands();
    }

    @Override
    public String getName() {
        return "DiscordBot";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        reload();

        if (botToken != null && !botToken.isEmpty() && !botToken.equals("INSERT_TOKEN_HERE")) {
            connectBot();
        } else {
            plugin.getLogManager().logWarn("[Premium Discord] Bot token not configured correctly");
        }
    }

    @Override
    public void disable() {
        disconnectBot();
        this.enabled = false;
        plugin.getLogManager().logInfo("[Premium Discord] Bot module disabled");
    }

    @Override
    public void reload() {
        this.botToken = plugin.getConfig().getString("modules.DiscordBot.token", "");
        this.alertChannelId = plugin.getConfig().getString("modules.DiscordBot.alert-channel", "");
    }

    private void connectBot() {
        if (jda != null)
            return;

        CompletableFuture.runAsync(() -> {
            try {
                this.jda = JDABuilder.createDefault(botToken, 
                        Arrays.asList(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT))
                        .setActivity(Activity.watching("SoulGuard Security"))
                        .build()
                        .awaitReady();

                plugin.getLogManager()
                        .logInfo("[Premium Discord] JDA Bot connected successfully as " + jda.getSelfUser().getName());
            } catch (InterruptedException e) {
                plugin.getLogManager().logError("[Premium Discord] Connection interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                plugin.getLogManager().logError("[Premium Discord] Failed to connect bot: " + e.getMessage());
            }
        });
    }

    private void disconnectBot() {
        if (jda != null) {
            jda.shutdown();
            jda = null;
            plugin.getLogManager().logInfo("[Premium Discord] JDA Bot logged out.");
        }
    }

    private void registerCommands() {
        // Register Discord commands
        commands.put("ban", new BanCommand());
        commands.put("mute", new MuteCommand());
        commands.put("kick", new KickCommand());
        commands.put("list", new ListCommand());
        commands.put("status", new StatusCommand());
    }

    /**
     * Sends an alert to Discord.
     */
    public void sendAlert(String title, String message, int color) {
        if (!enabled || alertChannelId == null || alertChannelId.isEmpty())
            return;

        CompletableFuture.runAsync(() -> {
            try {
                if (jda != null) {
                    String channelId = alertChannelId;
                    if (channelId == null) return;
                    net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel = jda
                            .getTextChannelById(channelId);
                    if (channel != null) {
                        net.dv8tion.jda.api.EmbedBuilder eb = new net.dv8tion.jda.api.EmbedBuilder();
                        eb.setTitle(title);
                        eb.setDescription(message);
                        eb.setColor(color);
                        channel.sendMessageEmbeds(eb.build()).queue();
                    }
                }
            } catch (Exception e) {
                plugin.getLogManager().logError("[Premium Discord] Failed to send alert: " + e.getMessage());
            }
        });
    }

    /**
     * Executes a command from Discord.
     */
    public void executeCommand(String commandName, String[] args, Object discordUser) {
        DiscordCommand command = commands.get(commandName.toLowerCase());
        if (command != null) {
            command.execute(plugin, args, discordUser);
        }
    }

    /**
     * Base interface for Discord commands.
     */
    private interface DiscordCommand {
        void execute(SoulGuard plugin, String[] args, Object discordUser);
    }

    private static class BanCommand implements DiscordCommand {
        @Override
        public void execute(SoulGuard plugin, String[] args, Object discordUser) {
            if (args.length < 1)
                return;
            String playerName = args[0];
            plugin.getLogManager().logInfo("[Premium Discord] Ban command received for: " + playerName);
            // In a real environment, this would call the punishment API
        }
    }

    private static class MuteCommand implements DiscordCommand {
        @Override
        public void execute(SoulGuard plugin, String[] args, Object discordUser) {
            if (args.length < 1)
                return;
            String playerName = args[0];
            plugin.getLogManager().logInfo("[Premium Discord] Mute command received for: " + playerName);
        }
    }

    private static class KickCommand implements DiscordCommand {
        @Override
        public void execute(SoulGuard plugin, String[] args, Object discordUser) {
            if (args.length < 1)
                return;
            String playerName = args[0];
            Player player = plugin.getServer().getPlayer(playerName);
            if (player != null) {
                player.kickPlayer("§cKicked by Discord staff");
                plugin.getLogManager().logInfo("[Premium Discord] Kicked " + playerName);
            }
        }
    }

    private static class ListCommand implements DiscordCommand {
        @Override
        public void execute(SoulGuard plugin, String[] args, Object discordUser) {
            int online = plugin.getServer().getOnlinePlayers().size();
            plugin.getLogManager().logInfo("[Premium Discord] List command processed (" + online + " online)");
        }
    }

    private static class StatusCommand implements DiscordCommand {
        @Override
        public void execute(SoulGuard plugin, String[] args, Object discordUser) {
            plugin.getLogManager().logInfo("[Premium Discord] Status command processed");
        }
    }
}
