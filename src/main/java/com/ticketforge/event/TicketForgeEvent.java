package com.ticketforge.event;

import java.time.Instant;

/**
 * Domain Event representing real-time state changes in TicketForge.
 */
public record TicketForgeEvent(
        String eventType,
        Integer seatNumber,
        String userId,
        String message,
        Instant timestamp
) {
    public static TicketForgeEvent of(String eventType, Integer seatNumber, String userId, String message) {
        return new TicketForgeEvent(eventType, seatNumber, userId, message, Instant.now());
    }
}
