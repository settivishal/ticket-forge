package com.ticketforge.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketforge.dto.InitializeSeatsRequest;
import com.ticketforge.dto.ReservationRequest;
import com.ticketforge.dto.SystemStatusResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End Multi-Protocol Integration Test Suite.
 * Validates cross-protocol lifecycle: REST -> GraphQL -> Auto-Waitlist -> REST Cancellation -> Auto-Promotion.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureGraphQlTester
@ActiveProfiles("dev")
class TicketForgeE2EIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("E2E Booking Lifecycle: REST init -> REST & GraphQL booking -> Waitlist -> Cancellation & Promotion")
    void testEndToEndCrossProtocolLifecycle() throws Exception {
        // 1. Admin initializes venue with 3 seats via REST
        InitializeSeatsRequest initReq = new InitializeSeatsRequest(3);
        mockMvc.perform(post("/api/v1/seats/initialize")
                        .header("Authorization", "Bearer dev-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSeats").value(3))
                .andExpect(jsonPath("$.data.availableSeats").value(3));

        // 2. Customer 1 books Seat 1 via REST
        ReservationRequest res1 = new ReservationRequest("usr_e2e_1", 2);
        mockMvc.perform(post("/api/v1/reservations")
                        .header("Authorization", "Bearer dev-customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(res1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.seatNumber").value(1))
                .andExpect(jsonPath("$.data.userId").value("usr_e2e_1"));

        // 3. Customer 2 books Seat 2 via GraphQL Mutation
        String gqlReserve = """
            mutation {
                reserveSeat(userId: "usr_e2e_2", priority: 2) {
                    seatNumber
                    userId
                    tier
                }
            }
        """;
        graphQlTester.document(gqlReserve)
                .execute()
                .path("reserveSeat.seatNumber").entity(Integer.class).isEqualTo(2)
                .path("reserveSeat.userId").entity(String.class).isEqualTo("usr_e2e_2");

        // 4. Customer 3 holds Seat 3 via GraphQL Mutation
        String gqlHold = """
            mutation {
                holdSeat(userId: "usr_e2e_3", priority: 1, ttlSeconds: 120) {
                    seatNumber
                    userId
                    tier
                }
            }
        """;
        graphQlTester.document(gqlHold)
                .execute()
                .path("holdSeat.seatNumber").entity(Integer.class).isEqualTo(3)
                .path("holdSeat.userId").entity(String.class).isEqualTo("usr_e2e_3");

        // 5. Customer 4 tries to book when capacity is 0 -> Enqueued in Waitlist (HTTP 202)
        ReservationRequest res4 = new ReservationRequest("usr_e2e_4", 3);
        mockMvc.perform(post("/api/v1/reservations")
                        .header("Authorization", "Bearer dev-customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(res4)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message", Matchers.containsString("added to priority waitlist")));

        // 6. Verify System Status via REST GET /api/v1/seats/availability
        mockMvc.perform(get("/api/v1/seats/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSeats").value(3))
                .andExpect(jsonPath("$.data.availableSeats").value(0))
                .andExpect(jsonPath("$.data.reservedSeats").value(2))
                .andExpect(jsonPath("$.data.heldSeats").value(1))
                .andExpect(jsonPath("$.data.waitlistCount").value(1));

        // 7. Verify GraphQL systemStatus query reflects identical counts
        String gqlStatus = """
            query {
                systemStatus {
                    totalSeats
                    availableSeats
                    waitlistCount
                }
            }
        """;
        SystemStatusResponse gqlStatusResp = graphQlTester.document(gqlStatus)
                .execute()
                .path("systemStatus")
                .entity(SystemStatusResponse.class)
                .get();
        assertThat(gqlStatusResp.totalSeats()).isEqualTo(3);
        assertThat(gqlStatusResp.availableSeats()).isEqualTo(0);
        assertThat(gqlStatusResp.waitlistCount()).isEqualTo(1);

        // 8. Customer 1 cancels Seat 1 via REST -> Auto-Promotes Customer 4 from waitlist to Seat 1
        mockMvc.perform(delete("/api/v1/reservations/1?userId=usr_e2e_1")
                        .header("Authorization", "Bearer dev-customer")
                        .header("X-Dev-User", "usr_e2e_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 9. Verify Waitlist is now 0 and Seat 1 is occupied by usr_e2e_4
        mockMvc.perform(get("/api/v1/seats/availability"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.waitlistCount").value(0));

        String gqlSeat1 = """
            query {
                seat(seatNumber: 1) {
                    seatNumber
                    status
                    occupantUserId
                }
            }
        """;
        graphQlTester.document(gqlSeat1)
                .execute()
                .path("seat.seatNumber").entity(Integer.class).isEqualTo(1)
                .path("seat.occupantUserId").entity(String.class).isEqualTo("usr_e2e_4");
    }

    @Test
    @DisplayName("RFC 7807 Error Contract: Invalid request payload returns standard ProblemDetails")
    void testRfc7807ProblemDetailsContract() throws Exception {
        // Attempting to reserve with empty userId
        ReservationRequest invalidReq = new ReservationRequest("", 2);

        mockMvc.perform(post("/api/v1/reservations")
                        .header("Authorization", "Bearer dev-customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.status").value(400));
    }
}
