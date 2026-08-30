package com.ticketforge.repository;

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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("dev")
class SeatRepositoryTest {

    @Autowired
    private SeatRepository seatRepository;

    @BeforeEach
    void setUp() {
        seatRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save and find seat by seat number")
    void testSaveAndFindBySeatNumber() {
        Seat seat = Seat.builder()
                .seatNumber(1)
                .status(SeatStatus.AVAILABLE)
                .tier(SeatTier.VIP)
                .build();

        Seat saved = seatRepository.save(seat);
        assertThat(saved.getId()).isNotNull();

        Optional<Seat> found = seatRepository.findBySeatNumber(1);
        assertThat(found).isPresent();
        assertThat(found.get().getTier()).isEqualTo(SeatTier.VIP);
        assertThat(found.get().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Should enforce unique seat number constraint")
    void testUniqueSeatNumberConstraint() {
        Seat seat1 = Seat.builder().seatNumber(10).status(SeatStatus.AVAILABLE).build();
        seatRepository.saveAndFlush(seat1);

        Seat seat2 = Seat.builder().seatNumber(10).status(SeatStatus.AVAILABLE).build();
        assertThatThrownBy(() -> seatRepository.saveAndFlush(seat2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should count seats by status correctly")
    void testCountByStatus() {
        seatRepository.save(Seat.builder().seatNumber(1).status(SeatStatus.AVAILABLE).build());
        seatRepository.save(Seat.builder().seatNumber(2).status(SeatStatus.AVAILABLE).build());
        seatRepository.save(Seat.builder().seatNumber(3).status(SeatStatus.RESERVED).build());
        seatRepository.save(Seat.builder().seatNumber(4).status(SeatStatus.HELD).build());

        assertThat(seatRepository.countByStatus(SeatStatus.AVAILABLE)).isEqualTo(2);
        assertThat(seatRepository.countByStatus(SeatStatus.RESERVED)).isEqualTo(1);
        assertThat(seatRepository.countByStatus(SeatStatus.HELD)).isEqualTo(1);
    }

    @Test
    @DisplayName("Should find available seats ordered by seat number ascending with lock")
    void testFindByStatusWithLock() {
        seatRepository.save(Seat.builder().seatNumber(5).status(SeatStatus.AVAILABLE).build());
        seatRepository.save(Seat.builder().seatNumber(2).status(SeatStatus.AVAILABLE).build());
        seatRepository.save(Seat.builder().seatNumber(8).status(SeatStatus.AVAILABLE).build());
        seatRepository.save(Seat.builder().seatNumber(1).status(SeatStatus.RESERVED).build());

        List<Seat> availableSeats = seatRepository.findByStatusWithLock(SeatStatus.AVAILABLE);
        assertThat(availableSeats).hasSize(3);
        assertThat(availableSeats.get(0).getSeatNumber()).isEqualTo(2);
        assertThat(availableSeats.get(1).getSeatNumber()).isEqualTo(5);
        assertThat(availableSeats.get(2).getSeatNumber()).isEqualTo(8);
    }

    @Test
    @DisplayName("Should find max seat number")
    void testFindMaxSeatNumber() {
        assertThat(seatRepository.findMaxSeatNumber()).isEmpty();

        seatRepository.save(Seat.builder().seatNumber(10).status(SeatStatus.AVAILABLE).build());
        seatRepository.save(Seat.builder().seatNumber(25).status(SeatStatus.AVAILABLE).build());
        seatRepository.save(Seat.builder().seatNumber(15).status(SeatStatus.AVAILABLE).build());

        Optional<Integer> maxSeat = seatRepository.findMaxSeatNumber();
        assertThat(maxSeat).contains(25);
    }
}
