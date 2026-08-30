package com.ticketforge.exception;

public class UserAlreadyInWaitlistException extends TicketForgeException {
    public UserAlreadyInWaitlistException(String message) {
        super(message);
    }
}
