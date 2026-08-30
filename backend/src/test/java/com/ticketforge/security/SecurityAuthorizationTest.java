package com.ticketforge.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketforge.dto.ExpandSeatsRequest;
import com.ticketforge.dto.InitializeSeatsRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketForgeService ticketForgeService;

    @BeforeEach
    void setUp() {
        ticketForgeService.initializeSeats(10);
    }

    @Test
    @DisplayName("Public Endpoint: GET /api/v1/seats/availability accessible without authentication")
    void testPublicAvailabilityEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/seats/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Public Endpoint: Actuator /actuator/health accessible without authentication")
    void testPublicActuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Protected Customer Endpoint: POST /api/v1/reservations rejects unauthenticated request (401)")
    void testUnauthenticatedReservationRejected() throws Exception {
        ReservationRequest request = new ReservationRequest("anon_user", 1);

        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Protected Customer Endpoint: GET /api/v1/seats rejects unauthenticated request (401)")
    void testUnauthenticatedSeatsListRejected() throws Exception {
        mockMvc.perform(get("/api/v1/seats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin Endpoint: POST /api/v1/seats/initialize rejects customer role with 403 Forbidden")
    @WithMockUser(roles = "CUSTOMER")
    void testCustomerForbiddenOnInitialize() throws Exception {
        InitializeSeatsRequest request = new InitializeSeatsRequest(50);

        mockMvc.perform(post("/api/v1/seats/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin Endpoint: POST /api/v1/seats/expand rejects customer role with 403 Forbidden")
    @WithMockUser(roles = "CUSTOMER")
    void testCustomerForbiddenOnExpand() throws Exception {
        ExpandSeatsRequest request = new ExpandSeatsRequest(10);

        mockMvc.perform(post("/api/v1/seats/expand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin Endpoint: POST /api/v1/reservations/release-range rejects customer role with 403 Forbidden")
    @WithMockUser(roles = "CUSTOMER")
    void testCustomerForbiddenOnReleaseRange() throws Exception {
        ReleaseSeatsRequest request = new ReleaseSeatsRequest("usr_1", "usr_5");

        mockMvc.perform(post("/api/v1/reservations/release-range")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin Endpoint: POST /api/v1/seats/initialize succeeds with ADMIN role (200)")
    @WithMockUser(roles = "ADMIN")
    void testAdminAuthorizedOnInitialize() throws Exception {
        InitializeSeatsRequest request = new InitializeSeatsRequest(30);

        mockMvc.perform(post("/api/v1/seats/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
