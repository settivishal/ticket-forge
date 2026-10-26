package com.ticketforge.event;

import java.time.Instant;

/**
 * Domain Event representing real-time state changes in TicketForge.
 * <p>
 * Broadcast publicly over SSE and GraphQL subscriptions, so it never carries user ids.
 */
public record TicketForgeEvent(
        String eventType,
        Integer seatNumber,
        String message,
        Instant timestamp
) {
    public static TicketForgeEvent of(String eventType, Integer seatNumber, String message) {
        return new TicketForgeEvent(eventType, seatNumber, message, Instant.now());
    }
}
