package com.ticketforge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body to dynamically add seats to venue capacity")
public record ExpandSeatsRequest(
        @Schema(description = "Number of additional seats to add to total inventory", example = "20")
        @NotNull(message = "Additional count is required")
        @Min(value = 1, message = "Additional count must be at least 1")
        @Max(value = 10000, message = "Additional count cannot exceed 10,000 at a time")
        Integer additionalCount
) {
}
