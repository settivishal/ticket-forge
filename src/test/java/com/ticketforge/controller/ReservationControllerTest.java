package com.ticketforge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketforge.dto.ReleaseSeatsRequest;
import com.ticketforge.dto.ReservationRequest;
import com.ticketforge.service.TicketForgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketForgeService ticketForgeService;

    @BeforeEach
    void setUp() {
        ticketForgeService.initializeSeats(2);
    }

    @Test
    @DisplayName("POST /api/v1/reservations - successfully reserves seat (201 Created)")
    @WithMockUser(roles = "CUSTOMER")
    void testReserveSeatSuccess() throws Exception {
        ReservationRequest request = new ReservationRequest("usr_rest_1", 2);

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.seatNumber").value(1))
                .andExpect(jsonPath("$.data.userId").value("usr_rest_1"));
    }

    @Test
    @DisplayName("POST /api/v1/reservations - full capacity places user in waitlist (202 Accepted)")
    @WithMockUser(roles = "CUSTOMER")
    void testReserveSeatWaitlist() throws Exception {
        // Book all 2 seats
        ticketForgeService.reserveSeat("usr_1", 1);
        ticketForgeService.reserveSeat("usr_2", 1);

        ReservationRequest request = new ReservationRequest("usr_waitlist_1", 3);

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message", containsString("added to priority waitlist")));
    }

    @Test
    @DisplayName("DELETE /api/v1/reservations/{seatNumber} - successfully cancels reservation")
    @WithMockUser(roles = "CUSTOMER")
    void testCancelReservation() throws Exception {
        ticketForgeService.reserveSeat("usr_cancel_1", 1);

        mockMvc.perform(delete("/api/v1/reservations/1")
                        .param("userId", "usr_cancel_1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message", containsString("cancelled successfully")));
    }

    @Test
    @DisplayName("GET /api/v1/reservations - lists active reservations")
    @WithMockUser(roles = "CUSTOMER")
    void testGetAllReservations() throws Exception {
        ticketForgeService.reserveSeat("usr_active_1", 1);

        mockMvc.perform(get("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].userId").value("usr_active_1"));
    }

    @Test
    @DisplayName("GET /api/v1/reservations/user/{userId} - gets active reservation for user")
    @WithMockUser(roles = "CUSTOMER")
    void testGetReservationByUserId() throws Exception {
        ticketForgeService.reserveSeat("usr_lookup_1", 1);

        mockMvc.perform(get("/api/v1/reservations/user/usr_lookup_1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("usr_lookup_1"))
                .andExpect(jsonPath("$.data.seatNumber").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/reservations/release-range - Admin can batch release reservations")
    @WithMockUser(roles = "ADMIN")
    void testReleaseRangeAsAdmin() throws Exception {
        ticketForgeService.reserveSeat("usr_10", 1);
        ticketForgeService.reserveSeat("usr_20", 1);

        ReleaseSeatsRequest request = new ReleaseSeatsRequest("usr_00", "usr_99");

        mockMvc.perform(post("/api/v1/reservations/release-range")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("POST /api/v1/reservations/release-range - Customer role is Forbidden (403)")
    @WithMockUser(roles = "CUSTOMER")
    void testReleaseRangeAsCustomerForbidden() throws Exception {
        ReleaseSeatsRequest request = new ReleaseSeatsRequest("usr_00", "usr_99");

        mockMvc.perform(post("/api/v1/reservations/release-range")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
