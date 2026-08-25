package com.ticketforge.service;

import com.ticketforge.dto.ReservationResponse;
import com.ticketforge.dto.SeatResponse;
import com.ticketforge.dto.SystemStatusResponse;
import com.ticketforge.dto.WaitlistResponse;
import com.ticketforge.exception.InvalidRequestException;
import com.ticketforge.exception.ReservationNotFoundException;
import com.ticketforge.exception.UserAlreadyInWaitlistException;
import com.ticketforge.exception.UserAlreadyReservedException;
import com.ticketforge.model.Seat;
import com.ticketforge.model.SeatStatus;
import com.ticketforge.model.SeatTier;
import com.ticketforge.repository.ReservationRepository;
import com.ticketforge.repository.SeatRepository;
import com.ticketforge.repository.UserRepository;
import com.ticketforge.repository.WaitlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class TicketForgeServiceTest {

    @Autowired
    private TicketForgeService ticketForgeService;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private WaitlistRepository waitlistRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        ticketForgeService.initializeSeats(5);
    }

    @Test
    @DisplayName("Should initialize venue with correct seat count and tiers")
    void testInitializeSeats() {
        SystemStatusResponse status = ticketForgeService.getSystemStatus();
        assertThat(status.totalSeats()).isEqualTo(5);
        assertThat(status.availableSeats()).isEqualTo(5);
        assertThat(status.reservedSeats()).isEqualTo(0);
        assertThat(status.waitlistCount()).isEqualTo(0);

        List<SeatResponse> seats = ticketForgeService.getAllSeats();
        assertThat(seats).hasSize(5);
        assertThat(seats.get(0).seatNumber()).isEqualTo(1);
        assertThat(seats.get(0).status()).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Should reject invalid seat count initialization")
    void testInvalidSeatInitialization() {
        assertThatThrownBy(() -> ticketForgeService.initializeSeats(0))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("Should reserve lowest available seat number in O(log N)")
    void testReserveSeatWhenAvailable() {
        ReservationResponse res1 = ticketForgeService.reserveSeat("user_101", 1);
        assertThat(res1).isNotNull();
        assertThat(res1.userId()).isEqualTo("user_101");
        assertThat(res1.seatNumber()).isEqualTo(1);

        ReservationResponse res2 = ticketForgeService.reserveSeat("user_102", 2);
        assertThat(res2).isNotNull();
        assertThat(res2.userId()).isEqualTo("user_102");
        assertThat(res2.seatNumber()).isEqualTo(2);

        SystemStatusResponse status = ticketForgeService.getSystemStatus();
        assertThat(status.availableSeats()).isEqualTo(3);
        assertThat(status.reservedSeats()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should automatically place user into priority waitlist when venue is sold out")
    void testReserveSeatWhenSoldOutPlacesInWaitlist() {
        // Book all 5 seats
        for (int i = 1; i <= 5; i++) {
            ticketForgeService.reserveSeat("user_" + i, 1);
        }

        // 6th user tries to reserve with priority 3 (VIP)
        ReservationResponse waitlistRes = ticketForgeService.reserveSeat("vip_user_6", 3);
        assertThat(waitlistRes).isNull(); // Indicates placed on waitlist

        SystemStatusResponse status = ticketForgeService.getSystemStatus();
        assertThat(status.availableSeats()).isEqualTo(0);
        assertThat(status.reservedSeats()).isEqualTo(5);
        assertThat(status.waitlistCount()).isEqualTo(1);

        List<WaitlistResponse> waitlist = ticketForgeService.getWaitlist();
        assertThat(waitlist).hasSize(1);
        assertThat(waitlist.get(0).userId()).isEqualTo("vip_user_6");
        assertThat(waitlist.get(0).priority()).isEqualTo(3);
        assertThat(waitlist.get(0).queuePosition()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should reject duplicate reservation for same user")
    void testDuplicateReservationRejection() {
        ticketForgeService.reserveSeat("user_dup", 1);

        assertThatThrownBy(() -> ticketForgeService.reserveSeat("user_dup", 2))
                .isInstanceOf(UserAlreadyReservedException.class);
    }

    @Test
    @DisplayName("Should reject duplicate waitlist entry for same user")
    void testDuplicateWaitlistRejection() {
        for (int i = 1; i <= 5; i++) {
            ticketForgeService.reserveSeat("user_" + i, 1);
        }

        ticketForgeService.reserveSeat("user_wait", 1);

        assertThatThrownBy(() -> ticketForgeService.reserveSeat("user_wait", 3))
                .isInstanceOf(UserAlreadyInWaitlistException.class);
    }

    @Test
    @DisplayName("Should hold seat with TTL expiration")
    void testHoldSeatWithTtl() {
        ReservationResponse hold = ticketForgeService.holdSeat("user_hold", 1, 300);
        assertThat(hold).isNotNull();
        assertThat(hold.expiresAt()).isNotNull();
        assertThat(hold.seatNumber()).isEqualTo(1);

        SeatResponse seat = ticketForgeService.getSeatByNumber(1);
        assertThat(seat.status()).isEqualTo(SeatStatus.HELD);
    }

    @Test
    @DisplayName("Should cancel reservation and immediately auto-promote highest-priority waiting customer")
    void testCancelReservationWithAutoPromotion() {
        // Book all 5 seats
        for (int i = 1; i <= 5; i++) {
            ticketForgeService.reserveSeat("user_" + i, 1);
        }

        // Add 2 users to waitlist: user_A (priority 1), user_B (priority 3 - VIP)
        ticketForgeService.reserveSeat("user_A", 1);
        ticketForgeService.reserveSeat("user_B", 3);

        // Cancel user_1's reservation for seat 1
        ticketForgeService.cancelReservation(1, "user_1");

        // Seat 1 must be auto-promoted to user_B (highest priority 3)!
        ReservationResponse resSeat1 = ticketForgeService.getReservationByUserId("user_B");
        assertThat(resSeat1).isNotNull();
        assertThat(resSeat1.seatNumber()).isEqualTo(1);
        assertThat(resSeat1.userId()).isEqualTo("user_B");

        // Waitlist size should now be 1 (user_A remaining)
        assertThat(ticketForgeService.getWaitlist()).hasSize(1);
        assertThat(ticketForgeService.getWaitlist().get(0).userId()).isEqualTo("user_A");
    }

    @Test
    @DisplayName("Should cancel reservation with empty waitlist and return seat to available inventory")
    void testCancelReservationWithEmptyWaitlist() {
        ticketForgeService.reserveSeat("user_single", 1);
        assertThat(ticketForgeService.getSystemStatus().availableSeats()).isEqualTo(4);

        ticketForgeService.cancelReservation(1, "user_single");

        assertThat(ticketForgeService.getSystemStatus().availableSeats()).isEqualTo(5);
        assertThat(ticketForgeService.getSeatByNumber(1).status()).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Should exit waitlist successfully")
    void testExitWaitlist() {
        for (int i = 1; i <= 5; i++) {
            ticketForgeService.reserveSeat("user_" + i, 1);
        }
        ticketForgeService.reserveSeat("user_wait_exit", 2);
        assertThat(ticketForgeService.getSystemStatus().waitlistCount()).isEqualTo(1);

        boolean removed = ticketForgeService.exitWaitlist("user_wait_exit");
        assertThat(removed).isTrue();
        assertThat(ticketForgeService.getSystemStatus().waitlistCount()).isEqualTo(0);

        boolean nonExistent = ticketForgeService.exitWaitlist("non_existent");
        assertThat(nonExistent).isFalse();
    }

    @Test
    @DisplayName("Should update priority in waitlist and dynamically re-order queue")
    void testUpdatePriorityInWaitlist() {
        for (int i = 1; i <= 5; i++) {
            ticketForgeService.reserveSeat("user_" + i, 1);
        }

        ticketForgeService.reserveSeat("user_low", 1);
        ticketForgeService.reserveSeat("user_med", 2);

        // Before update: user_med is position 1, user_low is position 2
        List<WaitlistResponse> waitlistBefore = ticketForgeService.getWaitlist();
        assertThat(waitlistBefore.get(0).userId()).isEqualTo("user_med");

        // Upgrade user_low priority from 1 to 5
        boolean updated = ticketForgeService.updatePriority("user_low", 5);
        assertThat(updated).isTrue();

        // After update: user_low is position 1!
        List<WaitlistResponse> waitlistAfter = ticketForgeService.getWaitlist();
        assertThat(waitlistAfter.get(0).userId()).isEqualTo("user_low");
        assertThat(waitlistAfter.get(0).priority()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should add seats and auto-fulfill pending waitlist entries")
    void testAddSeatsWithAutoFulfillment() {
        // Book all 5 seats
        for (int i = 1; i <= 5; i++) {
            ticketForgeService.reserveSeat("user_" + i, 1);
        }

        ticketForgeService.reserveSeat("wait_user_1", 2);
        ticketForgeService.reserveSeat("wait_user_2", 1);
        assertThat(ticketForgeService.getSystemStatus().waitlistCount()).isEqualTo(2);

        // Add 3 more seats (seats 6, 7, 8)
        ticketForgeService.addSeats(3);

        // wait_user_1 and wait_user_2 should be auto-promoted to seats 6 and 7!
        assertThat(ticketForgeService.getReservationByUserId("wait_user_1").seatNumber()).isEqualTo(6);
        assertThat(ticketForgeService.getReservationByUserId("wait_user_2").seatNumber()).isEqualTo(7);

        // 1 seat remaining available (seat 8)
        SystemStatusResponse status = ticketForgeService.getSystemStatus();
        assertThat(status.totalSeats()).isEqualTo(8);
        assertThat(status.availableSeats()).isEqualTo(1);
        assertThat(status.waitlistCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should release reservations in user range [fromUserId, toUserId] via Red-Black Tree scan")
    void testReleaseSeatsRange() {
        ticketForgeService.reserveSeat("usr_10", 1);
        ticketForgeService.reserveSeat("usr_20", 1);
        ticketForgeService.reserveSeat("usr_30", 1);

        List<Integer> released = ticketForgeService.releaseSeats("usr_10", "usr_25");
        assertThat(released).containsExactlyInAnyOrder(1, 2);

        assertThat(ticketForgeService.getSeatByNumber(1).status()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(ticketForgeService.getSeatByNumber(2).status()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(ticketForgeService.getSeatByNumber(3).status()).isEqualTo(SeatStatus.RESERVED);
    }

    @Test
    @DisplayName("Should hydrate in-memory DSAs from database on startup")
    void testSyncFromDatabase() {
        ticketForgeService.reserveSeat("usr_sync_1", 1);
        ticketForgeService.reserveSeat("usr_sync_2", 1);

        // Manually trigger syncFromDatabase
        ticketForgeService.syncFromDatabase();

        // Verify that in-memory reservations tree is hydrated
        assertThat(ticketForgeService.getReservationByUserId("usr_sync_1").seatNumber()).isEqualTo(1);
        assertThat(ticketForgeService.getReservationByUserId("usr_sync_2").seatNumber()).isEqualTo(2);
    }
}
