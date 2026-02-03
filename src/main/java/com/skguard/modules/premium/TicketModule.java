package com.skguard.modules.premium;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.skguard.SKGuard;
import com.skguard.api.SecurityModule;

/**
 * Premium-only Ticket System Module.
 * <p>
 * Manages support tickets and ban appeals.
 * Integrates with the Web Panel for remote management.
 * </p>
 * 
 * <p><b>Premium Feature</b> - Only available in SKGuard Premium</p>
 * 
 * @author SKGuard Team
 * @since 1.0
 */
public class TicketModule implements SecurityModule {

    private final SKGuard plugin;
    private boolean enabled;
    private final Map<UUID, Ticket> tickets;

    public TicketModule(SKGuard plugin) {
        this.plugin = plugin;
        this.tickets = new ConcurrentHashMap<>();
    }

    @Override
    public String getName() {
        return "Tickets";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void enable() {
        this.enabled = true;
        plugin.getLogManager().logInfo("[Premium] Ticket System enabled");
    }

    @Override
    public void disable() {
        tickets.clear();
        this.enabled = false;
        plugin.getLogManager().logInfo("[Premium] Ticket System disabled");
    }

    @Override
    public void reload() {
        // Load configuration
    }

    /**
     * Opens a new ticket.
     */
    public UUID openTicket(UUID creator, String subject, String message) {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = new Ticket(ticketId, creator, subject, message);
        tickets.put(ticketId, ticket);
        
        plugin.getLogManager().logInfo("[Premium Tickets] New ticket " + ticketId + " opened by " + creator);
        return ticketId;
    }

    /**
     * Gets all open tickets.
     */
    public List<Ticket> getActiveTickets() {
        return new ArrayList<>(tickets.values());
    }

    /**
     * Closes a ticket.
     */
    public void closeTicket(UUID ticketId) {
        tickets.remove(ticketId);
    }

    /**
     * Represents a support ticket.
     */
    public static class Ticket {
        private final UUID id;
        private final UUID creator;
        private final String subject;
        private final String message;
        private final long timestamp;
        private String status;

        public Ticket(UUID id, UUID creator, String subject, String message) {
            this.id = id;
            this.creator = creator;
            this.subject = subject;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
            this.status = "OPEN";
        }

        public UUID getId() { return id; }
        public UUID getCreator() { return creator; }
        public String getSubject() { return subject; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}

