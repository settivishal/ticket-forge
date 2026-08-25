package com.ticketforge.scheduler;

import com.ticketforge.model.Reservation;
import com.ticketforge.model.Seat;
import com.ticketforge.model.SeatStatus;
import com.ticketforge.repository.ReservationRepository;
import com.ticketforge.service.TicketForgeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketHoldTtlSchedulerTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private TicketForgeService ticketForgeService;

    @InjectMocks
    private TicketHoldTtlScheduler ttlScheduler;

    @Test
    @DisplayName("Should do nothing when no holds are expired")
    void testNoExpiredHolds() {
        when(reservationRepository.findExpiredHolds(any(Instant.class)))
                .thenReturn(Collections.emptyList());

        int count = ttlScheduler.processExpiredHolds();
        assertThat(count).isEqualTo(0);

        verify(ticketForgeService, never()).cancelReservation(any(Integer.class), any(String.class));
    }

    @Test
    @DisplayName("Should cancel reservation for all expired seat holds")
    void testProcessExpiredHolds() {
        Instant now = Instant.now();

        Seat seat1 = Seat.builder().id(1L).seatNumber(12).status(SeatStatus.HELD).build();
        Seat seat2 = Seat.builder().id(2L).seatNumber(15).status(SeatStatus.HELD).build();

        Reservation expired1 = Reservation.builder()
                .id(101L)
                .userId("user_expired_1")
                .seat(seat1)
                .expiresAt(now.minus(2, ChronoUnit.MINUTES))
                .build();

        Reservation expired2 = Reservation.builder()
                .id(102L)
                .userId("user_expired_2")
                .seat(seat2)
                .expiresAt(now.minus(10, ChronoUnit.SECONDS))
                .build();

        when(reservationRepository.findExpiredHolds(any(Instant.class)))
                .thenReturn(List.of(expired1, expired2));

        int processed = ttlScheduler.processExpiredHolds();
        assertThat(processed).isEqualTo(2);

        verify(ticketForgeService, times(1)).cancelReservation(eq(12), eq("user_expired_1"));
        verify(ticketForgeService, times(1)).cancelReservation(eq(15), eq("user_expired_2"));
    }
}
