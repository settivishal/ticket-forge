package com.ticketforge.scheduler;

import com.ticketforge.model.Reservation;
import com.ticketforge.repository.ReservationRepository;
import com.ticketforge.service.TicketForgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Background scheduler that monitors held seat reservations and automatically expires them
 * when their Time-to-Live (TTL) window elapses.
 * <p>
 * Releasing an expired hold immediately triggers cascading auto-promotion for waiting customers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TicketHoldTtlScheduler {

    private final ReservationRepository reservationRepository;
    private final TicketForgeService ticketForgeService;

    /**
     * Periodically runs every 5 seconds to scan for expired temporary holds.
     */
    @Scheduled(fixedRateString = "${ticketforge.ttl.cleanup-interval-ms:5000}")
    @Transactional
    public int processExpiredHolds() {
        Instant now = Instant.now();
        List<Reservation> expiredHolds = reservationRepository.findExpiredHolds(now);

        if (expiredHolds.isEmpty()) {
            return 0;
        }

        log.info("Found {} expired seat hold(s) to cleanup at {}", expiredHolds.size(), now);

        int processedCount = 0;
        for (Reservation hold : expiredHolds) {
            try {
                String userId = hold.getUserId();
                int seatNumber = hold.getSeat().getSeatNumber();

                log.warn("Expiring hold on seat {} for user {} (expired at {})", seatNumber, userId, hold.getExpiresAt());
                ticketForgeService.cancelReservation(seatNumber, userId);
                processedCount++;
            } catch (Exception e) {
                log.error("Failed to release expired hold for user {}: {}", hold.getUserId(), e.getMessage(), e);
            }
        }

        log.info("Successfully cleaned up and reallocated {} expired seat hold(s)", processedCount);
        return processedCount;
    }
}
