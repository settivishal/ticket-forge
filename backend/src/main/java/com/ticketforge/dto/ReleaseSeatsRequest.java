package com.ticketforge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request body for batch releasing user reservations in range [fromUserId, toUserId]")
public record ReleaseSeatsRequest(
        @Schema(description = "Starting user ID of the range", example = "usr_10")
        @NotBlank(message = "fromUserId is required")
        String fromUserId,

        @Schema(description = "Ending user ID of the range", example = "usr_25")
        @NotBlank(message = "toUserId is required")
        String toUserId
) {
}
