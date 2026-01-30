package com.soulguard.integration;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.soulguard.SoulGuard;
import com.soulguard.api.SecurityModule;

public class DiscordWebhook implements SecurityModule {

    private final SoulGuard plugin;
    private boolean enabled;
    private String webhookUrl;
    private final ExecutorService asyncExecutor;

    public DiscordWebhook(SoulGuard plugin) {
        this.plugin = plugin;
        this.asyncExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public String getName() {
        return "DiscordWebhook";
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
        asyncExecutor.shutdown();
    }

    @Override
    public void reload() {
        this.webhookUrl = plugin.getConfig().getString("modules.DiscordWebhook.url", "");
        if (this.webhookUrl.isEmpty() || this.webhookUrl.equals("INSERT_WEBHOOK_HERE")) {
            this.enabled = false;
            plugin.getLogManager().logInfo("DiscordWebhook disabled: No valid URL provided.");
        } else {
            this.enabled = true;
            plugin.getLogManager().logInfo("DiscordWebhook enabled and URL loaded successfully.");
        }
    }

    public void sendAlert(String title, String description, int color) {
        if (!enabled || webhookUrl.isEmpty())
            return;

        asyncExecutor.submit(() -> {
            try {
                String json = String.format(
                        "{\"embeds\":[{\"title\":\"%s\",\"description\":\"%s\",\"color\":%d,\"footer\":{\"text\":\"SoulGuard Security\"}}]}",
                        escapeJson(title), escapeJson(description), color);

                URL url = URI.create(webhookUrl).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "SoulGuard-Plugin");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    plugin.getLogManager().logWarn("Failed to send Discord Webhook. Code: " + responseCode);
                }
            } catch (java.io.IOException | IllegalArgumentException e) {
                plugin.getLogManager().logError("Error sending Discord Webhook: " + e.getMessage());
            }
        });
    }

    public void sendSimpleEmbed(String title, String message, String colorHex) {
        if (!enabled || webhookUrl.isEmpty())
            return;

        asyncExecutor.submit(() -> {
            try {
                int color = Integer.parseInt(colorHex, 16);
                String json = String.format(
                        "{\"embeds\":[{\"title\":\"%s\",\"description\":\"%s\",\"color\":%d}]}",
                        escapeJson(title), escapeJson(message), color);

                URL url = URI.create(webhookUrl).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    // Fail silently but acknowledge locally for debugging if needed
                }
                connection.disconnect();
            } catch (java.io.IOException | IllegalArgumentException ignored) {
                // Silently fail to avoid console spam
            }
        });
    }

    private String escapeJson(String text) {
        if (text == null)
            return "";
        return text.replace("\"", "\\\"").replace("\n", "\\n");
    }
}
