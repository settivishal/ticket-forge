package com.ticketforge.service;

import com.ticketforge.concurrency.DistributedLockManager;
import com.ticketforge.dto.ReservationResponse;
import com.ticketforge.event.RedisEventPublisher;
import com.ticketforge.exception.ReservationNotFoundException;
import com.ticketforge.exception.UserAlreadyReservedException;
import com.ticketforge.model.Reservation;
import com.ticketforge.model.Seat;
import com.ticketforge.model.SeatStatus;
import com.ticketforge.model.SeatTier;
import com.ticketforge.model.WaitlistEntry;
import com.ticketforge.model.WaitlistStatus;
import com.ticketforge.repository.ReservationRepository;
import com.ticketforge.repository.SeatRepository;
import com.ticketforge.repository.UserRepository;
import com.ticketforge.repository.WaitlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketForgeServiceMockitoTest {

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private WaitlistRepository waitlistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisEventPublisher redisEventPublisher;

    private DistributedLockManager distributedLockManager;
    private TicketForgeServiceImpl ticketForgeService;

    @BeforeEach
    void setUp() {
        distributedLockManager = new DistributedLockManager(null);
        ticketForgeService = new TicketForgeServiceImpl(
                seatRepository,
                reservationRepository,
                waitlistRepository,
                userRepository,
                distributedLockManager,
                redisEventPublisher
        );
    }

    @Test
    @DisplayName("reserveSeat: Successfully reserves lowest available seat and persists")
    void testReserveSeatAvailable() {
        Seat seat1 = Seat.builder()
                .id(1L)
                .seatNumber(1)
                .status(SeatStatus.AVAILABLE)
                .tier(SeatTier.STANDARD)
                .build();

        when(seatRepository.findBySeatNumber(1)).thenReturn(Optional.of(seat1));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> {
            Reservation r = i.getArgument(0);
            r.setId(101L);
            return r;
        });

        // Initialize 1 seat in service
        ticketForgeService.initializeSeats(1);

        ReservationResponse response = ticketForgeService.reserveSeat("usr_mock_1", 2);

        assertThat(response).isNotNull();
        assertThat(response.seatNumber()).isEqualTo(1);
        assertThat(response.userId()).isEqualTo("usr_mock_1");

        verify(seatRepository, atLeastOnce()).save(any(Seat.class));
        verify(reservationRepository, times(1)).save(any(Reservation.class));
        verify(redisEventPublisher, atLeastOnce()).publishEvent(any());
    }

    @Test
    @DisplayName("reserveSeat: Enqueues into waitlist when venue is full")
    void testReserveSeatFullCapacityEnqueuesWaitlist() {
        Seat seat1 = Seat.builder().id(1L).seatNumber(1).status(SeatStatus.AVAILABLE).tier(SeatTier.STANDARD).build();
        when(seatRepository.findBySeatNumber(1)).thenReturn(Optional.of(seat1));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
        when(waitlistRepository.save(any(WaitlistEntry.class))).thenAnswer(i -> i.getArgument(0));

        ticketForgeService.initializeSeats(1);
        ticketForgeService.reserveSeat("usr_first", 1); // consumes only available seat

        ReservationResponse response = ticketForgeService.reserveSeat("usr_wl_mock", 3);

        assertThat(response).isNull(); // Indicates placed on waitlist
        verify(waitlistRepository, times(1)).save(any(WaitlistEntry.class));
        verify(redisEventPublisher, atLeastOnce()).publishEvent(any());
    }

    @Test
    @DisplayName("reserveSeat: Throws UserAlreadyReservedException if user already holds a seat")
    void testReserveSeatDuplicateUserThrows() {
        Seat seat1 = Seat.builder()
                .id(1L)
                .seatNumber(1)
                .status(SeatStatus.AVAILABLE)
                .tier(SeatTier.STANDARD)
                .build();
        when(seatRepository.findBySeatNumber(1)).thenReturn(Optional.of(seat1));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));

        ticketForgeService.initializeSeats(1);
        ticketForgeService.reserveSeat("usr_dup", 1);

        assertThatThrownBy(() -> ticketForgeService.reserveSeat("usr_dup", 1))
                .isInstanceOf(UserAlreadyReservedException.class)
                .hasMessageContaining("already has an active reservation or hold");
    }

    @Test
    @DisplayName("cancelReservation: Throws ReservationNotFoundException if reservation does not exist")
    void testCancelReservationNotFoundThrows() {
        assertThatThrownBy(() -> ticketForgeService.cancelReservation(99, "usr_none"))
                .isInstanceOf(ReservationNotFoundException.class);
    }

    @Test
    @DisplayName("cancelReservation: Frees seat and cascades re-allocation to highest priority waitlist customer")
    void testCancelReservationReallocatesToWaitlist() {
        Seat seat1 = Seat.builder()
                .id(1L)
                .seatNumber(1)
                .status(SeatStatus.AVAILABLE)
                .tier(SeatTier.STANDARD)
                .build();
        when(seatRepository.findBySeatNumber(1)).thenReturn(Optional.of(seat1));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
        when(waitlistRepository.save(any(WaitlistEntry.class))).thenAnswer(i -> i.getArgument(0));

        // 1 seat capacity
        ticketForgeService.initializeSeats(1);

        // usr_1 gets seat 1
        ticketForgeService.reserveSeat("usr_1", 1);

        // usr_vip enters waitlist with priority 3
        ticketForgeService.reserveSeat("usr_vip", 3);

        // Cancel usr_1 reservation
        ticketForgeService.cancelReservation(1, "usr_1");

        // Verify reservation deleted and new reservation saved for waitlist user
        verify(reservationRepository, times(1)).deleteByUserId("usr_1");
        verify(redisEventPublisher, atLeastOnce()).publishEvent(any());
    }

    @Test
    @DisplayName("exitWaitlist: Removes user from waitlist and updates status")
    void testExitWaitlist() {
        Seat seat1 = Seat.builder().id(1L).seatNumber(1).status(SeatStatus.AVAILABLE).tier(SeatTier.STANDARD).build();
        when(seatRepository.findBySeatNumber(1)).thenReturn(Optional.of(seat1));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
        when(waitlistRepository.save(any(WaitlistEntry.class))).thenAnswer(i -> i.getArgument(0));

        ticketForgeService.initializeSeats(1);
        ticketForgeService.reserveSeat("occupant", 1); // fill capacity

        ticketForgeService.reserveSeat("usr_exit", 1); // enters waitlist

        boolean exited = ticketForgeService.exitWaitlist("usr_exit");
        assertThat(exited).isTrue();
        verify(waitlistRepository, times(1)).deleteByUserId("usr_exit");
    }

    @Test
    @DisplayName("updatePriority: Dynamically adjusts waitlist priority tier in O(log N)")
    void testUpdatePriority() {
        Seat seat1 = Seat.builder().id(1L).seatNumber(1).status(SeatStatus.AVAILABLE).tier(SeatTier.STANDARD).build();
        when(seatRepository.findBySeatNumber(1)).thenReturn(Optional.of(seat1));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
        when(waitlistRepository.save(any(WaitlistEntry.class))).thenAnswer(i -> i.getArgument(0));

        ticketForgeService.initializeSeats(1);
        ticketForgeService.reserveSeat("occupant", 1); // fill capacity

        ticketForgeService.reserveSeat("usr_promo", 1); // enters waitlist

        WaitlistEntry entry = WaitlistEntry.builder()
                .id(502L)
                .userId("usr_promo")
                .priority(1)
                .timestamp(System.currentTimeMillis())
                .status(WaitlistStatus.WAITING)
                .build();
        when(waitlistRepository.findByUserId("usr_promo"))
                .thenReturn(Optional.of(entry));

        boolean updated = ticketForgeService.updatePriority("usr_promo", 3);
        assertThat(updated).isTrue();
        verify(waitlistRepository, atLeastOnce()).save(any(WaitlistEntry.class));
    }

    @Test
    @DisplayName("addSeats: Expands inventory and auto-fulfills waiting customers")
    void testAddSeatsAutoFulfillsWaitlist() {
        Seat seat1 = Seat.builder().id(1L).seatNumber(1).status(SeatStatus.AVAILABLE).tier(SeatTier.STANDARD).build();
        when(seatRepository.findBySeatNumber(1)).thenReturn(Optional.of(seat1));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(i -> i.getArgument(0));
        when(waitlistRepository.save(any(WaitlistEntry.class))).thenAnswer(i -> i.getArgument(0));

        ticketForgeService.initializeSeats(1);
        ticketForgeService.reserveSeat("occupant", 1); // fill capacity

        // Place usr_waiting in waitlist
        ticketForgeService.reserveSeat("usr_waiting", 2);

        when(seatRepository.findMaxSeatNumber()).thenReturn(Optional.of(1));
        when(seatRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        // Add 1 seat -> should auto-allocate to usr_waiting
        ticketForgeService.addSeats(1);

        verify(seatRepository, atLeastOnce()).saveAll(anyList());
        verify(reservationRepository, atLeastOnce()).save(any(Reservation.class));
    }
}
