package com.ticketforge.exception;

public class RateLimitExceededException extends TicketForgeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
