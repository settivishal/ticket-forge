package com.ticketforge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class TicketForgeApplicationTests {

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("Application context should load successfully")
    void contextLoads() {
    }
}
