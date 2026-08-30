package com.ticketforge.exception;

public class UserAlreadyReservedException extends TicketForgeException {
    public UserAlreadyReservedException(String message) {
        super(message);
    }
}
