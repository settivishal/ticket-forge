package com.ticketforge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body to reserve a seat or join the priority waitlist")
public record ReservationRequest(
        @Schema(description = "User unique identifier (UUID or username)", example = "usr_101")
        @NotBlank(message = "User ID is required")
        String userId,

        @Schema(description = "Priority level (1 = Standard, 2 = Premium, 3 = VIP)", example = "1", defaultValue = "1")
        @Min(value = 1, message = "Priority must be at least 1")
        @Max(value = 5, message = "Priority cannot exceed 5")
        Integer priority
) {
    public ReservationRequest {
        if (priority == null) {
            priority = 1;
        }
    }
}
