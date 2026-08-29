package com.ticketforge.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketforge.dto.InitializeSeatsRequest;
import com.ticketforge.dto.ReservationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DevAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Dev Auth: 'Authorization: Bearer dev-admin' grants ROLE_ADMIN to initialize seats")
    void testBearerDevAdminInitializesSeats() throws Exception {
        InitializeSeatsRequest request = new InitializeSeatsRequest(40);

        mockMvc.perform(post("/api/v1/seats/initialize")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSeats").value(40));
    }

    @Test
    @DisplayName("Dev Auth: 'X-Dev-Role: ADMIN' grants ROLE_ADMIN to initialize seats")
    void testXDevRoleAdminInitializesSeats() throws Exception {
        InitializeSeatsRequest request = new InitializeSeatsRequest(35);

        mockMvc.perform(post("/api/v1/seats/initialize")
                        .header("X-Dev-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSeats").value(35));
    }

    @Test
    @DisplayName("Dev Auth: 'Authorization: Bearer dev-customer' allows reserving seats as customer")
    void testBearerDevCustomerReservesSeat() throws Exception {
        // Initialize venue first
        InitializeSeatsRequest initReq = new InitializeSeatsRequest(10);
        mockMvc.perform(post("/api/v1/seats/initialize")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReq)))
                .andExpect(status().isOk());

        ReservationRequest request = new ReservationRequest("dev_usr_55", 2);

        mockMvc.perform(post("/api/v1/reservations")
                        .header("Authorization", "Bearer dev-customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("dev_usr_55"));
    }

    @Test
    @DisplayName("Dev Auth: 'Authorization: Bearer dev-customer' is forbidden on admin endpoint")
    void testDevCustomerForbiddenOnAdminEndpoint() throws Exception {
        InitializeSeatsRequest request = new InitializeSeatsRequest(10);

        mockMvc.perform(post("/api/v1/seats/initialize")
                        .header("Authorization", "Bearer dev-customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
