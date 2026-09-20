package com.ticketforge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

/**
 * Request body to reserve a seat or join the priority waitlist.
 * <p>
 * The acting user and their priority tier are taken from the authenticated principal,
 * never from the request, so neither appears here.
 */
@Schema(description = "Request body to reserve a seat or join the priority waitlist")
public record ReservationRequest(
        @Schema(description = "Specific seat to reserve. Omit to be allocated the best available seat.",
                example = "42", nullable = true)
        @Min(value = 1, message = "Seat number must be at least 1")
        Integer seatNumber
) {
}
