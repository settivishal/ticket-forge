package com.ticketforge.controller;

import com.ticketforge.dto.ApiResponse;
import com.ticketforge.dto.ExpandSeatsRequest;
import com.ticketforge.dto.InitializeSeatsRequest;
import com.ticketforge.dto.SeatResponse;
import com.ticketforge.dto.SystemStatusResponse;
import com.ticketforge.service.TicketForgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for seat inventory management and venue availability.
 */
@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Seats", description = "Seat inventory management, expansion and availability endpoints")
public class SeatController {

    private final TicketForgeService ticketForgeService;

    @PostMapping("/initialize")
    @Operation(summary = "Initialize venue capacity", description = "Clears previous state and initializes total seat capacity (Admin only)")
    public ResponseEntity<ApiResponse<SystemStatusResponse>> initializeSeats(@Valid @RequestBody InitializeSeatsRequest request) {
        log.info("REST: Initializing venue with {} seats", request.seatCount());
        ticketForgeService.initializeSeats(request.seatCount());
        SystemStatusResponse status = ticketForgeService.getSystemStatus();
        return ResponseEntity.ok(ApiResponse.success("Initialized venue with " + request.seatCount() + " seats", status));
    }

    @PostMapping("/expand")
    @Operation(summary = "Expand seat inventory", description = "Adds additional seats to total capacity and auto-promotes waitlist (Admin only)")
    public ResponseEntity<ApiResponse<SystemStatusResponse>> expandSeats(@Valid @RequestBody ExpandSeatsRequest request) {
        log.info("REST: Expanding venue capacity by adding {} seats", request.additionalCount());
        ticketForgeService.addSeats(request.additionalCount());
        SystemStatusResponse status = ticketForgeService.getSystemStatus();
        return ResponseEntity.ok(ApiResponse.success("Successfully added " + request.additionalCount() + " seats", status));
    }

    @GetMapping("/availability")
    @Operation(summary = "Get system status & availability", description = "Returns real-time seat availability and waitlist depth")
    public ResponseEntity<ApiResponse<SystemStatusResponse>> getAvailability() {
        SystemStatusResponse status = ticketForgeService.getSystemStatus();
        return ResponseEntity.ok(ApiResponse.success("System status retrieved successfully", status));
    }

    @GetMapping
    @Operation(summary = "List all seats", description = "Retrieves all seats with status, tier, and assigned user ID")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getAllSeats() {
        List<SeatResponse> seats = ticketForgeService.getAllSeats();
        return ResponseEntity.ok(ApiResponse.success("Retrieved " + seats.size() + " seats", seats));
    }

    @GetMapping("/{seatNumber}")
    @Operation(summary = "Get seat details", description = "Retrieves status and details of a specific seat number")
    public ResponseEntity<ApiResponse<SeatResponse>> getSeatByNumber(@PathVariable int seatNumber) {
        SeatResponse seat = ticketForgeService.getSeatByNumber(seatNumber);
        return ResponseEntity.ok(ApiResponse.success("Seat " + seatNumber + " details retrieved", seat));
    }
}
