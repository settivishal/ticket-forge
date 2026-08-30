package com.ticketforge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class WaitlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketForgeService ticketForgeService;

    @BeforeEach
    void setUp() {
        ticketForgeService.initializeSeats(1);
        ticketForgeService.reserveSeat("usr_held_1", 1); // fills the only seat
        ticketForgeService.reserveSeat("usr_wl_1", 1);   // enters waitlist
    }

    @Test
    @DisplayName("GET /api/v1/waitlist - retrieves active waitlist")
    @WithMockUser(roles = "CUSTOMER")
    void testGetWaitlist() throws Exception {
        mockMvc.perform(get("/api/v1/waitlist")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].userId").value("usr_wl_1"))
                .andExpect(jsonPath("$.data[0].priority").value(1));
    }

    @Test
    @DisplayName("PATCH /api/v1/waitlist/{userId} - updates waitlist priority")
    @WithMockUser(roles = "CUSTOMER")
    void testUpdatePriority() throws Exception {
        UpdatePriorityRequest request = new UpdatePriorityRequest(3);

        mockMvc.perform(patch("/api/v1/waitlist/usr_wl_1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message", containsString("Priority updated to 3")));
    }

    @Test
    @DisplayName("DELETE /api/v1/waitlist/{userId} - removes user from waitlist")
    @WithMockUser(roles = "CUSTOMER")
    void testExitWaitlist() throws Exception {
        mockMvc.perform(delete("/api/v1/waitlist/usr_wl_1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message", containsString("removed from waitlist")));
    }
}
