package com.ticketforge.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class StaticUiDashboardTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("UI: Serves index.html dashboard with TicketForge title and seating elements")
    void testServesIndexHtml() throws Exception {
        mockMvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("TicketForge")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Interactive Arena Seating Chart")));
    }

    @Test
    @DisplayName("UI: Serves styles.css with modern design tokens")
    void testServesStylesCss() throws Exception {
        mockMvc.perform(get("/styles.css"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("brand-primary")));
    }

    @Test
    @DisplayName("UI: Serves app.js with SSE EventSource logic")
    void testServesAppJs() throws Exception {
        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("EventSource")));
    }
}
