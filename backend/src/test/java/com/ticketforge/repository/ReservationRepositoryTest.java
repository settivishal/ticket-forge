package com.ticketforge.repository;

import com.ticketforge.model.Reservation;
import com.ticketforge.model.Seat;
import com.ticketforge.model.SeatStatus;
import com.ticketforge.model.SeatTier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("dev")
class ReservationRepositoryTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private SeatRepository seatRepository;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
    }

    @Test
    @DisplayName("Should create reservation and increment optimistic locking version")
    void testOptimisticLockingVersion() {
        Seat seat = seatRepository.saveAndFlush(Seat.builder()
                .seatNumber(1)
                .status(SeatStatus.RESERVED)
                .tier(SeatTier.STANDARD)
                .build());

        Reservation res = Reservation.builder()
                .userId("usr_100")
                .seat(seat)
                .reservedAt(Instant.now())
                .build();

        Reservation saved = reservationRepository.saveAndFlush(res);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(0);

        // Update reservation to bump version
        saved.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        Reservation updated = reservationRepository.saveAndFlush(saved);
        assertThat(updated.getVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should find expired holds")
    void testFindExpiredHolds() {
        Instant now = Instant.now();

        Seat seatHeldExpired = seatRepository.saveAndFlush(Seat.builder()
                .seatNumber(1).status(SeatStatus.HELD).build());
        Seat seatHeldValid = seatRepository.saveAndFlush(Seat.builder()
                .seatNumber(2).status(SeatStatus.HELD).build());
        Seat seatReserved = seatRepository.saveAndFlush(Seat.builder()
                .seatNumber(3).status(SeatStatus.RESERVED).build());

        // Expired hold (expired 5 minutes ago)
        reservationRepository.saveAndFlush(Reservation.builder()
                .userId("user_expired")
                .seat(seatHeldExpired)
                .reservedAt(now.minus(10, ChronoUnit.MINUTES))
                .expiresAt(now.minus(5, ChronoUnit.MINUTES))
                .build());

        // Valid hold (expires in 5 minutes)
        reservationRepository.saveAndFlush(Reservation.builder()
                .userId("user_valid")
                .seat(seatHeldValid)
                .reservedAt(now)
                .expiresAt(now.plus(5, ChronoUnit.MINUTES))
                .build());

        // Confirmed reservation (no expiry)
        reservationRepository.saveAndFlush(Reservation.builder()
                .userId("user_reserved")
                .seat(seatReserved)
                .reservedAt(now)
                .build());

        List<Reservation> expired = reservationRepository.findExpiredHolds(now);
        assertThat(expired).hasSize(1);
        assertThat(expired.get(0).getUserId()).isEqualTo("user_expired");
    }

    @Test
    @DisplayName("Should enforce one reservation per user constraint")
    void testOneReservationPerUser() {
        Seat seat1 = seatRepository.saveAndFlush(Seat.builder().seatNumber(1).status(SeatStatus.RESERVED).build());
        Seat seat2 = seatRepository.saveAndFlush(Seat.builder().seatNumber(2).status(SeatStatus.RESERVED).build());

        reservationRepository.saveAndFlush(Reservation.builder()
                .userId("usr_single")
                .seat(seat1)
                .reservedAt(Instant.now())
                .build());

        assertThatThrownBy(() -> reservationRepository.saveAndFlush(Reservation.builder()
                .userId("usr_single")
                .seat(seat2)
                .reservedAt(Instant.now())
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should find reservations by user ID range")
    void testFindByUserIdBetween() {
        Seat seat1 = seatRepository.saveAndFlush(Seat.builder().seatNumber(1).status(SeatStatus.RESERVED).build());
        Seat seat2 = seatRepository.saveAndFlush(Seat.builder().seatNumber(2).status(SeatStatus.RESERVED).build());
        Seat seat3 = seatRepository.saveAndFlush(Seat.builder().seatNumber(3).status(SeatStatus.RESERVED).build());

        reservationRepository.saveAndFlush(Reservation.builder().userId("usr_10").seat(seat1).build());
        reservationRepository.saveAndFlush(Reservation.builder().userId("usr_20").seat(seat2).build());
        reservationRepository.saveAndFlush(Reservation.builder().userId("usr_30").seat(seat3).build());

        List<Reservation> range = reservationRepository.findByUserIdBetween("usr_10", "usr_25");
        assertThat(range).hasSize(2);
    }
}
