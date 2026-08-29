package com.ticketforge.controller;

import com.ticketforge.dto.ApiResponse;
import com.ticketforge.dto.UpdatePriorityRequest;
import com.ticketforge.dto.WaitlistResponse;
import com.ticketforge.exception.InvalidRequestException;
import com.ticketforge.service.TicketForgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for priority waitlist queue management.
 */
@RestController
@RequestMapping("/api/v1/waitlist")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Waitlist", description = "Priority waitlist queue querying, priority updates and removal")
public class WaitlistController {

    private final TicketForgeService ticketForgeService;

    @GetMapping
    @Operation(summary = "Get priority waitlist", description = "Retrieves active waitlist queue ordered by priority tier (descending) and timestamp (FIFO)")
    public ResponseEntity<ApiResponse<List<WaitlistResponse>>> getWaitlist() {
        List<WaitlistResponse> waitlist = ticketForgeService.getWaitlist();
        return ResponseEntity.ok(ApiResponse.success("Retrieved " + waitlist.size() + " waitlist entries", waitlist));
    }

    @PatchMapping("/{userId}")
    @Operation(summary = "Update waitlist priority", description = "Updates a user's priority level in the waitlist in O(log N) time")
    public ResponseEntity<ApiResponse<Void>> updatePriority(
            @PathVariable String userId,
            @Valid @RequestBody UpdatePriorityRequest request) {
        log.info("REST: Updating waitlist priority for userId={} to {}", userId, request.newPriority());
        boolean updated = ticketForgeService.updatePriority(userId, request.newPriority());
        if (!updated) {
            throw new InvalidRequestException("User '" + userId + "' is not currently in the waitlist");
        }
        return ResponseEntity.ok(ApiResponse.success("Priority updated to " + request.newPriority() + " for user " + userId));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Exit waitlist", description = "Removes a user from the priority waitlist")
    public ResponseEntity<ApiResponse<Void>> exitWaitlist(@PathVariable String userId) {
        log.info("REST: Removing userId={} from waitlist", userId);
        boolean removed = ticketForgeService.exitWaitlist(userId);
        if (!removed) {
            throw new InvalidRequestException("User '" + userId + "' is not currently in the waitlist");
        }
        return ResponseEntity.ok(ApiResponse.success("User " + userId + " removed from waitlist"));
    }
}
