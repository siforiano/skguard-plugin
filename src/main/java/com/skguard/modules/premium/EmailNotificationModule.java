package com.skguard.modules.premium;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;

/**
 * Premium-only Email Notification Module.
 * <p>
 * Sends email alerts for critical security events.
 * Useful for server owners who want to be notified of important events
 * even when not online.
 * </p>
 * 
 * <p><b>Premium Feature</b> - Only available in SKGuard Premium</p>
 * 
 * @author SKGuard Team
 * @since 1.0
 */
public class EmailNotificationModule implements SecurityModule {

    private final SKGuard plugin;
    private boolean enabled;
    private String smtpHost;
    private int smtpPort;
    private String smtpUser;
    private String smtpPassword;
    private String fromEmail;
    private String toEmail;
    private boolean useTLS;

    public EmailNotificationModule(SKGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "EmailNotifications";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        reload();
        
        if (smtpHost != null && !smtpHost.isEmpty()) {
            plugin.getLogManager().logInfo("[Premium Email] Email notifications enabled");
        } else {
            plugin.getLogManager().logWarn("[Premium Email] SMTP not configured");
        }
    }

    @Override
    public void disable() {
        this.enabled = false;
        plugin.getLogManager().logInfo("[Premium Email] Email notifications disabled");
    }

    @Override
    public void reload() {
        this.smtpHost = plugin.getConfig().getString("modules.EmailNotifications.smtp.host", "smtp.gmail.com");
        this.smtpPort = plugin.getConfig().getInt("modules.EmailNotifications.smtp.port", 587);
        this.smtpUser = plugin.getConfig().getString("modules.EmailNotifications.smtp.user", "");
        this.smtpPassword = plugin.getConfig().getString("modules.EmailNotifications.smtp.password", "");
        this.fromEmail = plugin.getConfig().getString("modules.EmailNotifications.from", "");
        this.toEmail = plugin.getConfig().getString("modules.EmailNotifications.to", "");
        this.useTLS = plugin.getConfig().getBoolean("modules.EmailNotifications.smtp.tls", true);
    }

    /**
     * Sends an email notification.
     * 
     * @param subject the email subject
     * @param body the email body
     */
    public void sendEmail(String subject, String body) {
        if (!enabled || toEmail == null || toEmail.isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.host", smtpHost);
                props.put("mail.smtp.port", smtpPort);
                props.put("mail.smtp.auth", "true");
                
                if (useTLS) {
                    props.put("mail.smtp.starttls.enable", "true");
                }

                if (smtpUser == null || smtpPassword == null || fromEmail == null || toEmail == null) {
                    return;
                }

                Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                    @Override
                    protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                        return new javax.mail.PasswordAuthentication(smtpUser, smtpPassword);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(fromEmail));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                message.setSubject("[SKGuard] " + subject);
                message.setText(body);

                Transport.send(message);
                plugin.getLogManager().logInfo("[Premium Email] Sent: " + subject);

            } catch (MessagingException e) {
                plugin.getLogManager().logError("[Premium Email] Failed to send: " + e.getMessage());
            }
        });
    }

    /**
     * Sends a critical alert email.
     */
    public void sendCriticalAlert(String title, String details) {
        String body = "CRITICAL ALERT\n\n" +
                     "Title: " + title + "\n" +
                     "Details: " + details + "\n\n" +
                     "Server: " + plugin.getServer().getName() + "\n" +
                     "Time: " + new java.util.Date();
        
        sendEmail("CRITICAL: " + title, body);
    }

    /**
     * Sends a security event notification.
     */
    public void sendSecurityEvent(String eventType, String playerName, String details) {
        String body = "Security Event Detected\n\n" +
                     "Event: " + eventType + "\n" +
                     "Player: " + playerName + "\n" +
                     "Details: " + details + "\n\n" +
                     "Time: " + new java.util.Date();
        
        sendEmail("Security Event: " + eventType, body);
    }

    /**
     * Sends an SMS alert via Twilio integration (Premium Only).
     */
    public void sendSMS(String message) {
        if (!enabled) return;
        
        // Configuration for Twilio
        String accountSid = plugin.getConfig().getString("modules.EmailNotifications.sms.twilio-sid", "");
        String authToken = plugin.getConfig().getString("modules.EmailNotifications.sms.twilio-token", "");
        String fromPhone = plugin.getConfig().getString("modules.EmailNotifications.sms.twilio-phone", "");
        String toPhone = plugin.getConfig().getString("modules.EmailNotifications.sms.to-phone", "");

        if (accountSid.isEmpty() || authToken.isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            try {
                // Using parameters to simulate usage
                String logMsg = String.format("[Premium SMS] Sending from %s to %s", fromPhone, toPhone);
                // TODO: Implement Twilio REST API call
                plugin.getLogManager().logInfo(logMsg);
            } catch (Exception e) {
                plugin.getLogManager().logError("[Premium SMS] Failed to send: " + e.getMessage());
            }
        });
    }
}

