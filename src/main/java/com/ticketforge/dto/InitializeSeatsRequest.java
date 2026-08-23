package com.ticketforge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body for venue seat inventory initialization")
public record InitializeSeatsRequest(
        @Schema(description = "Total number of seats to initialize in the venue", example = "100")
        @NotNull(message = "Seat count is required")
        @Min(value = 1, message = "Seat count must be at least 1")
        @Max(value = 100000, message = "Seat count cannot exceed 100,000")
        Integer seatCount
) {
}
