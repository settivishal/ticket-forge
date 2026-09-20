package com.ticketforge.scheduler;

import com.ticketforge.model.Reservation;
import com.ticketforge.repository.ReservationRepository;
import com.ticketforge.service.TicketForgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
     * Periodically scans for expired temporary holds.
     * <p>
     * Deliberately NOT {@code @Transactional}.
     * <p>
     * {@code cancelReservation} is itself transactional with REQUIRED propagation. If this
     * method opened a transaction, every cancellation would join it, and the first failure
     * would mark that shared transaction rollback-only — discarding the work done for holds
     * that had already succeeded, while the non-transactional in-memory structures kept
     * their mutations. Running without an outer transaction gives each cancellation its own,
     * so a per-item failure stays contained.
     */
    @Scheduled(fixedRateString = "${ticketforge.ttl.cleanup-interval-ms:5000}")
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

        if (processedCount < expiredHolds.size()) {
            log.warn("Released {} of {} expired seat hold(s); {} failed and will be retried on the next run",
                    processedCount, expiredHolds.size(), expiredHolds.size() - processedCount);
        } else {
            log.info("Successfully cleaned up and reallocated {} expired seat hold(s)", processedCount);
        }
        return processedCount;
    }
}
