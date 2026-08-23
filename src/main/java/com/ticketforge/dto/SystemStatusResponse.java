package com.ticketforge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "High-level summary of the entire venue ticketing system state")
public record SystemStatusResponse(
        @Schema(description = "Total number of initialized seats", example = "100")
        long totalSeats,

        @Schema(description = "Number of currently available seats", example = "42")
        long availableSeats,

        @Schema(description = "Number of currently held seats (pending TTL confirmation)", example = "8")
        long heldSeats,

        @Schema(description = "Number of confirmed reserved seats", example = "50")
        long reservedSeats,

        @Schema(description = "Number of active users in the priority waitlist", example = "14")
        long waitlistCount
) {
}
