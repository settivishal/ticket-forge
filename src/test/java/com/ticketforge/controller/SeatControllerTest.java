package com.ticketforge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketforge.dto.ExpandSeatsRequest;
import com.ticketforge.dto.InitializeSeatsRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SeatControllerTest {

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
    @DisplayName("GET /api/v1/seats/availability - returns public status and metrics without authentication")
    void testGetAvailabilityPublic() throws Exception {
        mockMvc.perform(get("/api/v1/seats/availability")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSeats").value(10))
                .andExpect(jsonPath("$.data.availableSeats").value(10))
                .andExpect(jsonPath("$.data.reservedSeats").value(0))
                .andExpect(jsonPath("$.data.waitlistCount").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/seats - retrieves all seats list with authenticated user")
    @WithMockUser(roles = "CUSTOMER")
    void testGetAllSeats() throws Exception {
        mockMvc.perform(get("/api/v1/seats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(10)))
                .andExpect(jsonPath("$.data[0].seatNumber").value(1))
                .andExpect(jsonPath("$.data[0].status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("GET /api/v1/seats/{seatNumber} - retrieves individual seat details")
    @WithMockUser(roles = "CUSTOMER")
    void testGetSeatByNumber() throws Exception {
        mockMvc.perform(get("/api/v1/seats/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.seatNumber").value(1))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("POST /api/v1/seats/initialize - Admin role can re-initialize venue capacity")
    @WithMockUser(roles = "ADMIN")
    void testInitializeSeatsAsAdmin() throws Exception {
        InitializeSeatsRequest request = new InitializeSeatsRequest(25);

        mockMvc.perform(post("/api/v1/seats/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSeats").value(25))
                .andExpect(jsonPath("$.data.availableSeats").value(25));
    }

    @Test
    @DisplayName("POST /api/v1/seats/initialize - Customer role is Forbidden (403)")
    @WithMockUser(roles = "CUSTOMER")
    void testInitializeSeatsAsCustomerForbidden() throws Exception {
        InitializeSeatsRequest request = new InitializeSeatsRequest(25);

        mockMvc.perform(post("/api/v1/seats/initialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/seats/expand - Admin role can expand venue seat inventory")
    @WithMockUser(roles = "ADMIN")
    void testExpandSeatsAsAdmin() throws Exception {
        ExpandSeatsRequest request = new ExpandSeatsRequest(5);

        mockMvc.perform(post("/api/v1/seats/expand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSeats").value(15))
                .andExpect(jsonPath("$.data.availableSeats").value(15));
    }

    @Test
    @DisplayName("POST /api/v1/seats/expand - Customer role is Forbidden (403)")
    @WithMockUser(roles = "CUSTOMER")
    void testExpandSeatsAsCustomerForbidden() throws Exception {
        ExpandSeatsRequest request = new ExpandSeatsRequest(5);

        mockMvc.perform(post("/api/v1/seats/expand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
