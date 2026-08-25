package com.ticketforge.graphql;

import com.ticketforge.dto.ReservationResponse;
import com.ticketforge.dto.SeatResponse;
import com.ticketforge.dto.SystemStatusResponse;
import com.ticketforge.dto.WaitlistResponse;
import com.ticketforge.model.SeatStatus;
import com.ticketforge.service.TicketForgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureGraphQlTester
@ActiveProfiles("dev")
class TicketForgeGraphQlTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private TicketForgeService ticketForgeService;

    @BeforeEach
    void setUp() {
        ticketForgeService.initializeSeats(20);
    }

    @Test
    @DisplayName("Query: systemStatus returns accurate system status aggregates")
    void testQuerySystemStatus() {
        String document = """
            query {
                systemStatus {
                    totalSeats
                    availableSeats
                    heldSeats
                    reservedSeats
                    waitlistCount
                }
            }
        """;

        SystemStatusResponse response = graphQlTester.document(document)
                .execute()
                .path("systemStatus")
                .entity(SystemStatusResponse.class)
                .get();

        assertThat(response).isNotNull();
        assertThat(response.totalSeats()).isEqualTo(20);
        assertThat(response.availableSeats()).isEqualTo(20);
        assertThat(response.reservedSeats()).isEqualTo(0);
        assertThat(response.waitlistCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Query: seats returns all venue seats")
    void testQuerySeats() {
        String document = """
            query {
                seats {
                    id
                    seatNumber
                    status
                    tier
                    occupantUserId
                }
            }
        """;

        List<SeatResponse> seats = graphQlTester.document(document)
                .execute()
                .path("seats")
                .entityList(SeatResponse.class)
                .get();

        assertThat(seats).hasSize(20);
        assertThat(seats.get(0).seatNumber()).isEqualTo(1);
        assertThat(seats.get(0).status()).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Query: seat returns specific seat details")
    void testQuerySeatByNumber() {
        String document = """
            query {
                seat(seatNumber: 1) {
                    seatNumber
                    status
                    tier
                }
            }
        """;

        SeatResponse seat = graphQlTester.document(document)
                .execute()
                .path("seat")
                .entity(SeatResponse.class)
                .get();

        assertThat(seat).isNotNull();
        assertThat(seat.seatNumber()).isEqualTo(1);
        assertThat(seat.status()).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Mutation: reserveSeat successfully books an available seat")
    void testMutationReserveSeat() {
        String document = """
            mutation {
                reserveSeat(userId: "usr_gql_1", priority: 3) {
                    userId
                    seatNumber
                    tier
                }
            }
        """;

        ReservationResponse reservation = graphQlTester.document(document)
                .execute()
                .path("reserveSeat")
                .entity(ReservationResponse.class)
                .get();

        assertThat(reservation).isNotNull();
        assertThat(reservation.userId()).isEqualTo("usr_gql_1");
        assertThat(reservation.seatNumber()).isEqualTo(1);

        SystemStatusResponse status = ticketForgeService.getSystemStatus();
        assertThat(status.availableSeats()).isEqualTo(19);
        assertThat(status.reservedSeats()).isEqualTo(1);
    }

    @Test
    @DisplayName("Mutation: holdSeat holds a seat with TTL")
    void testMutationHoldSeat() {
        String document = """
            mutation {
                holdSeat(userId: "usr_gql_hold", priority: 2, ttlSeconds: 120) {
                    userId
                    seatNumber
                    expiresAt
                }
            }
        """;

        ReservationResponse hold = graphQlTester.document(document)
                .execute()
                .path("holdSeat")
                .entity(ReservationResponse.class)
                .get();

        assertThat(hold).isNotNull();
        assertThat(hold.userId()).isEqualTo("usr_gql_hold");
        assertThat(hold.expiresAt()).isNotNull();

        SystemStatusResponse status = ticketForgeService.getSystemStatus();
        assertThat(status.heldSeats()).isEqualTo(1);
    }

    @Test
    @DisplayName("Mutation: cancelReservation frees seat or cascades to waitlist")
    void testMutationCancelReservation() {
        ticketForgeService.reserveSeat("usr_gql_cancel", 1);

        String document = """
            mutation {
                cancelReservation(seatNumber: 1, userId: "usr_gql_cancel")
            }
        """;

        Boolean result = graphQlTester.document(document)
                .execute()
                .path("cancelReservation")
                .entity(Boolean.class)
                .get();

        assertThat(result).isTrue();
        SystemStatusResponse status = ticketForgeService.getSystemStatus();
        assertThat(status.availableSeats()).isEqualTo(20);
    }

    @Test
    @DisplayName("Query & Mutation: Waitlist flow via GraphQL")
    void testWaitlistFlow() {
        ticketForgeService.initializeSeats(1);
        ticketForgeService.reserveSeat("usr_first", 1);
        ticketForgeService.reserveSeat("usr_wait1", 2);

        String queryWaitlistDoc = """
            query {
                waitlist {
                    userId
                    priority
                    queuePosition
                }
            }
        """;

        List<WaitlistResponse> waitlist = graphQlTester.document(queryWaitlistDoc)
                .execute()
                .path("waitlist")
                .entityList(WaitlistResponse.class)
                .get();

        assertThat(waitlist).hasSize(1);
        assertThat(waitlist.get(0).userId()).isEqualTo("usr_wait1");

        String updatePriorityDoc = """
            mutation {
                updatePriority(userId: "usr_wait1", priority: 5)
            }
        """;

        Boolean updated = graphQlTester.document(updatePriorityDoc)
                .execute()
                .path("updatePriority")
                .entity(Boolean.class)
                .get();

        assertThat(updated).isTrue();

        String exitWaitlistDoc = """
            mutation {
                exitWaitlist(userId: "usr_wait1")
            }
        """;

        Boolean exited = graphQlTester.document(exitWaitlistDoc)
                .execute()
                .path("exitWaitlist")
                .entity(Boolean.class)
                .get();

        assertThat(exited).isTrue();
    }

    @Test
    @DisplayName("Mutation: initializeSeats re-initializes venue capacity")
    void testMutationInitializeSeats() {
        String document = """
            mutation {
                initializeSeats(count: 50) {
                    totalSeats
                    availableSeats
                }
            }
        """;

        SystemStatusResponse response = graphQlTester.document(document)
                .execute()
                .path("initializeSeats")
                .entity(SystemStatusResponse.class)
                .get();

        assertThat(response.totalSeats()).isEqualTo(50);
        assertThat(response.availableSeats()).isEqualTo(50);
    }

    @Test
    @DisplayName("Mutation: addSeats expands venue inventory")
    void testMutationAddSeats() {
        String document = """
            mutation {
                addSeats(count: 10) {
                    totalSeats
                    availableSeats
                }
            }
        """;

        SystemStatusResponse response = graphQlTester.document(document)
                .execute()
                .path("addSeats")
                .entity(SystemStatusResponse.class)
                .get();

        assertThat(response.totalSeats()).isEqualTo(30);
        assertThat(response.availableSeats()).isEqualTo(30);
    }

    @Test
    @DisplayName("Mutation: releaseSeats batch-releases reservations")
    void testMutationReleaseSeats() {
        ticketForgeService.reserveSeat("usr_a", 1);
        ticketForgeService.reserveSeat("usr_b", 1);

        String document = """
            mutation {
                releaseSeats(fromUserId: "usr_a", toUserId: "usr_b")
            }
        """;

        List<Integer> releasedSeats = graphQlTester.document(document)
                .execute()
                .path("releaseSeats")
                .entityList(Integer.class)
                .get();

        assertThat(releasedSeats).isNotEmpty();
    }
}
