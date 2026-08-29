package com.ticketforge.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketforge.dto.InitializeSeatsRequest;
import com.ticketforge.dto.ReservationRequest;
import com.ticketforge.dto.UpdatePriorityRequest;
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
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketForgeService ticketForgeService;

    @BeforeEach
    void setUp() {
        ticketForgeService.initializeSeats(5);
    }

    @Test
    @DisplayName("404 Not Found - querying non-existent seat returns RFC 7807 ProblemDetail")
    @WithMockUser(roles = "CUSTOMER")
    void testSeatNotFoundProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/seats/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Seat Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail", containsString("999")))
                .andExpect(jsonPath("$.type").value("https://ticketforge.com/errors/seat-not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("404 Not Found - querying non-existent user reservation returns RFC 7807 ProblemDetail")
    @WithMockUser(roles = "CUSTOMER")
    void testReservationNotFoundProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/user/non_existent_user")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Reservation Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("409 Conflict - duplicate reservation by same user returns RFC 7807 ProblemDetail")
    @WithMockUser(roles = "CUSTOMER")
    void testUserAlreadyReservedProblemDetail() throws Exception {
        ReservationRequest request = new ReservationRequest("usr_duplicate_1", 1);

        // First reservation succeeds
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second reservation returns 409 Conflict ProblemDetail
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("User Already Has Reservation"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value("https://ticketforge.com/errors/user-already-reserved"));
    }

    @Test
    @DisplayName("400 Bad Request - validation failure returns RFC 7807 ProblemDetail with invalidParams")
    @WithMockUser(roles = "ADMIN")
    void testValidationFailureProblemDetail() throws Exception {
        // seatCount cannot be less than 1
        InitializeSeatsRequest request = new InitializeSeatsRequest(-10);

        mockMvc.perform(post("/api/v1/seats/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.invalidParams.seatCount").exists());
    }

    @Test
    @DisplayName("400 Bad Request - invalid operation returns RFC 7807 ProblemDetail")
    @WithMockUser(roles = "CUSTOMER")
    void testInvalidRequestProblemDetail() throws Exception {
        UpdatePriorityRequest request = new UpdatePriorityRequest(3);

        mockMvc.perform(patch("/api/v1/waitlist/non_existent_waitlist_user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request"))
                .andExpect(jsonPath("$.status").value(400));
    }
}
