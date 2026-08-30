package com.ticketforge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request body to update a user's priority in the waitlist")
public record UpdatePriorityRequest(
        @Schema(description = "New priority level (1 to 5)", example = "3")
        @NotNull(message = "New priority is required")
        @Min(value = 1, message = "Priority must be at least 1")
        @Max(value = 5, message = "Priority cannot exceed 5")
        Integer newPriority
) {
}
