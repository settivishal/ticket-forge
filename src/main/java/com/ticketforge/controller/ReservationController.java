package com.ticketforge.controller;

import com.ticketforge.dto.ApiResponse;
import com.ticketforge.dto.ReleaseSeatsRequest;
import com.ticketforge.dto.ReservationRequest;
import com.ticketforge.dto.ReservationResponse;
import com.ticketforge.service.TicketForgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for seat reservations, cancellations, and batch seat releases.
 */
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reservations", description = "Seat reservation booking, cancellation, and release operations")
public class ReservationController {

    private final TicketForgeService ticketForgeService;

    @PostMapping
    @Operation(summary = "Reserve a seat or enter waitlist", description = "Allocates lowest available seat or automatically enqueues user in priority waitlist")
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveSeat(@Valid @RequestBody ReservationRequest request) {
        log.info("REST: Reservation request for userId={}, priority={}", request.userId(), request.priority());
        ReservationResponse response = ticketForgeService.reserveSeat(request.userId(), request.priority());

        if (response != null) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Seat " + response.seatNumber() + " successfully reserved", response));
        } else {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success("Venue at full capacity. User " + request.userId() + " added to priority waitlist", null));
        }
    }

    @DeleteMapping("/{seatNumber}")
    @Operation(summary = "Cancel reservation", description = "Cancels a reservation and cascades re-allocation to highest-priority waitlist customer")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
            @PathVariable int seatNumber,
            @RequestParam(required = false) String userId) {
        log.info("REST: Cancellation request for seatNumber={}, userId={}", seatNumber, userId);
        ticketForgeService.cancelReservation(seatNumber, userId);
        return ResponseEntity.ok(ApiResponse.success("Reservation for seat " + seatNumber + " cancelled successfully"));
    }

    @GetMapping
    @Operation(summary = "List all reservations", description = "Retrieves all active confirmed and held reservations")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getAllReservations() {
        List<ReservationResponse> reservations = ticketForgeService.getAllReservations();
        return ResponseEntity.ok(ApiResponse.success("Retrieved " + reservations.size() + " reservations", reservations));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get reservation by user ID", description = "Retrieves active reservation for a specific user ID")
    public ResponseEntity<ApiResponse<ReservationResponse>> getReservationByUserId(@PathVariable String userId) {
        ReservationResponse reservation = ticketForgeService.getReservationByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Reservation found for user " + userId, reservation));
    }

    @PostMapping("/release-range")
    @Operation(summary = "Batch release seat reservations", description = "Releases all reservations for users in range [fromUserId, toUserId] (Admin only)")
    public ResponseEntity<ApiResponse<List<Integer>>> releaseSeats(@Valid @RequestBody ReleaseSeatsRequest request) {
        log.info("REST: Batch release request for user range [{}, {}]", request.fromUserId(), request.toUserId());
        List<Integer> releasedSeats = ticketForgeService.releaseSeats(request.fromUserId(), request.toUserId());
        return ResponseEntity.ok(ApiResponse.success("Released " + releasedSeats.size() + " seats for user range [" + request.fromUserId() + ", " + request.toUserId() + "]", releasedSeats));
    }
}
