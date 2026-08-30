package com.ticketforge.dto;

import com.ticketforge.model.SeatStatus;
import com.ticketforge.model.SeatTier;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representation of a venue seat state")
public record SeatResponse(
        @Schema(description = "Seat database ID", example = "1")
        Long id,

        @Schema(description = "Seat number in the venue", example = "12")
        Integer seatNumber,

        @Schema(description = "Current availability status", example = "RESERVED")
        SeatStatus status,

        @Schema(description = "Seat tier", example = "VIP")
        SeatTier tier,

        @Schema(description = "User ID who booked/held this seat (null if available)", example = "usr_101")
        String userId
) {
}
