package com.jomariabejo.connectly_api.ticketing_api.exception;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(Long id) {
        super("Ticket not found: " + id);
    }
}
