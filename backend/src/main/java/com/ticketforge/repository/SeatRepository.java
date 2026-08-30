package com.ticketforge.repository;

import com.ticketforge.model.Seat;
import com.ticketforge.model.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    Optional<Seat> findBySeatNumber(Integer seatNumber);

    boolean existsBySeatNumber(Integer seatNumber);

    long countByStatus(SeatStatus status);

    List<Seat> findByStatusOrderBySeatNumberAsc(SeatStatus status);

    List<Seat> findAllByOrderBySeatNumberAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.seatNumber = :seatNumber")
    Optional<Seat> findBySeatNumberWithLock(@Param("seatNumber") Integer seatNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.status = :status ORDER BY s.seatNumber ASC")
    List<Seat> findByStatusWithLock(@Param("status") SeatStatus status);

    @Query("SELECT MAX(s.seatNumber) FROM Seat s")
    Optional<Integer> findMaxSeatNumber();
}
