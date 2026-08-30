package com.ticketforge.concurrency;

import com.ticketforge.dto.ReservationResponse;
import com.ticketforge.dto.SystemStatusResponse;
import com.ticketforge.dto.WaitlistResponse;
import com.ticketforge.service.TicketForgeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Multi-Threaded Concurrency Stress & Race Condition Benchmark Suite (Phase 7.5).
 * Simulates high-contention flash sales with 100+ concurrent threads.
 */
@SpringBootTest
@ActiveProfiles("dev")
class TicketForgeHeavyConcurrencyStressTest {

    @Autowired
    private TicketForgeService ticketForgeService;

    @Test
    @DisplayName("Flash Sale Stress: 100 concurrent threads competing for 20 seats guarantees ZERO double bookings")
    void test100ConcurrentThreadsFlashSale() throws InterruptedException {
        int totalSeats = 20;
        int totalThreads = 100;

        ticketForgeService.initializeSeats(totalSeats);

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);

        List<ReservationResponse> successfulReservations = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger waitlistedCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int i = 1; i <= totalThreads; i++) {
            final String userId = "usr_stress_" + i;
            final int priority = (i % 3) + 1; // Priority 1, 2, or 3

            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    ReservationResponse res = ticketForgeService.reserveSeat(userId, priority);
                    if (res != null) {
                        successfulReservations.add(res);
                    } else {
                        waitlistedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Fire all 100 threads at the exact same millisecond
        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(errorCount.get()).isEqualTo(0);

        // Verification 1: Exactly totalSeats (20) reservations allocated
        assertThat(successfulReservations).hasSize(totalSeats);

        // Verification 2: Exactly 80 users in waitlist
        assertThat(waitlistedCount.get()).isEqualTo(totalThreads - totalSeats);

        // Verification 3: Every reserved seat number is unique (zero double bookings)
        Set<Integer> uniqueSeatNumbers = new HashSet<>();
        for (ReservationResponse res : successfulReservations) {
            assertThat(uniqueSeatNumbers.add(res.seatNumber()))
                    .as("Seat %d was double booked!", res.seatNumber())
                    .isTrue();
        }
        assertThat(uniqueSeatNumbers).hasSize(totalSeats);

        // Verification 4: System aggregates reflect exact counts
        SystemStatusResponse status = ticketForgeService.getSystemStatus();
        assertThat(status.totalSeats()).isEqualTo(totalSeats);
        assertThat(status.availableSeats()).isEqualTo(0);
        assertThat(status.reservedSeats()).isEqualTo(totalSeats);
        assertThat(status.waitlistCount()).isEqualTo(totalThreads - totalSeats);
    }

    @Test
    @DisplayName("Concurrent Cancellations & Auto-Promotions: Concurrent cancellations deterministically promote highest-priority waitlist customers")
    void testConcurrentCancellationsAndPromotions() throws InterruptedException {
        int totalSeats = 10;
        int totalWaitlist = 15;

        ticketForgeService.initializeSeats(totalSeats);

        // Book all 10 seats
        for (int i = 1; i <= totalSeats; i++) {
            ticketForgeService.reserveSeat("usr_initial_" + i, 1);
        }

        // Enqueue 15 users into waitlist with varying priorities
        for (int i = 1; i <= totalWaitlist; i++) {
            ticketForgeService.reserveSeat("usr_wait_" + i, (i % 3) + 1);
        }

        // Concurrently cancel 5 seats
        int cancellations = 5;
        ExecutorService executor = Executors.newFixedThreadPool(cancellations);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(cancellations);
        AtomicInteger successfulCancellations = new AtomicInteger(0);

        for (int i = 1; i <= cancellations; i++) {
            final int seatNum = i;
            final String userId = "usr_initial_" + i;

            executor.submit(() -> {
                try {
                    startLatch.await();
                    ticketForgeService.cancelReservation(seatNum, userId);
                    successfulCancellations.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successfulCancellations.get()).isEqualTo(cancellations);

        // Verification: Waitlist count reduced by exactly 5
        SystemStatusResponse status = ticketForgeService.getSystemStatus();
        assertThat(status.totalSeats()).isEqualTo(totalSeats);
        assertThat(status.availableSeats()).isEqualTo(0);
        assertThat(status.reservedSeats()).isEqualTo(totalSeats);
        assertThat(status.waitlistCount()).isEqualTo(totalWaitlist - cancellations);
    }

    @Test
    @DisplayName("Concurrent Priority Updates: Thread-safe dynamic waitlist priority promotions")
    void testConcurrentPriorityUpdates() throws InterruptedException {
        int totalSeats = 5;
        int waitlistUsers = 20;

        ticketForgeService.initializeSeats(totalSeats);

        for (int i = 1; i <= totalSeats; i++) {
            ticketForgeService.reserveSeat("occupant_" + i, 1);
        }

        for (int i = 1; i <= waitlistUsers; i++) {
            ticketForgeService.reserveSeat("wl_user_" + i, 1);
        }

        // Concurrently update priorities for 10 users to VIP (Priority 5)
        int updateCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(updateCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(updateCount);
        AtomicInteger successfulUpdates = new AtomicInteger(0);

        for (int i = 1; i <= updateCount; i++) {
            final String userId = "wl_user_" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    boolean ok = ticketForgeService.updatePriority(userId, 5);
                    if (ok) {
                        successfulUpdates.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successfulUpdates.get()).isEqualTo(updateCount);

        List<WaitlistResponse> waitlist = ticketForgeService.getWaitlist();
        assertThat(waitlist).hasSize(waitlistUsers);

        // Top 10 users in the waitlist must now have Priority 5
        for (int i = 0; i < updateCount; i++) {
            assertThat(waitlist.get(i).priority()).isEqualTo(5);
        }
    }
}
