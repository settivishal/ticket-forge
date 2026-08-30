package com.ticketforge.dto;

import com.ticketforge.model.WaitlistStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Representation of a user's position in the priority waitlist queue")
public record WaitlistResponse(
        @Schema(description = "Waitlist entry database ID", example = "1")
        Long id,

        @Schema(description = "User ID in the waitlist", example = "usr_402")
        String userId,

        @Schema(description = "User priority level", example = "3")
        Integer priority,

        @Schema(description = "Timestamp when the user joined the waitlist")
        Long timestamp,

        @Schema(description = "Waitlist status", example = "WAITING")
        WaitlistStatus status,

        @Schema(description = "Calculated position in the priority queue (1-based)", example = "1")
        Integer queuePosition
) {
}
