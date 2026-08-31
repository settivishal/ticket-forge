package com.ticketforge.exception;

/**
 * Raised when a specifically requested seat has already been taken.
 * Maps to HTTP 409 so the client can prompt the user to choose again.
 */
public class SeatUnavailableException extends TicketForgeException {
    public SeatUnavailableException(String message) {
        super(message);
    }
}
