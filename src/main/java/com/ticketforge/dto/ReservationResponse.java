package com.ticketforge.dto;

import com.ticketforge.model.SeatTier;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Representation of a confirmed seat reservation")
public record ReservationResponse(
        @Schema(description = "Reservation database ID", example = "1")
        Long id,

        @Schema(description = "User ID who holds the reservation", example = "usr_101")
        String userId,

        @Schema(description = "Reserved seat number", example = "12")
        Integer seatNumber,

        @Schema(description = "Seat tier", example = "STANDARD")
        SeatTier tier,

        @Schema(description = "Timestamp when the reservation was created")
        Instant reservedAt,

        @Schema(description = "Timestamp when the hold expires (if in HELD status)")
        Instant expiresAt
) {
}
