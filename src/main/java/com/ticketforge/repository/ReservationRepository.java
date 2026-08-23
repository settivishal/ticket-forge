package com.ticketforge.repository;

import com.ticketforge.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByUserId(String userId);

    Optional<Reservation> findBySeat_SeatNumber(Integer seatNumber);

    List<Reservation> findAllByOrderBySeat_SeatNumberAsc();

    void deleteBySeat_SeatNumber(Integer seatNumber);

    void deleteByUserId(String userId);

    boolean existsByUserId(String userId);

    boolean existsBySeat_SeatNumber(Integer seatNumber);

    @Query("SELECT r FROM Reservation r WHERE r.expiresAt IS NOT NULL AND r.expiresAt < :now AND r.seat.status = com.ticketforge.model.SeatStatus.HELD")
    List<Reservation> findExpiredHolds(@Param("now") Instant now);

    @Query("SELECT r FROM Reservation r WHERE r.userId >= :fromUserId AND r.userId <= :toUserId")
    List<Reservation> findByUserIdBetween(@Param("fromUserId") String fromUserId, @Param("toUserId") String toUserId);
}
