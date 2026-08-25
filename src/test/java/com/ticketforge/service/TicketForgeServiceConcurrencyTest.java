package com.ticketforge.service;

import com.ticketforge.dto.ReservationResponse;
import com.ticketforge.dto.SystemStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class TicketForgeServiceConcurrencyTest {

    @Autowired
    private TicketForgeService ticketForgeService;

    @BeforeEach
    void setUp() {
        // Initialize venue with 20 seats
        ticketForgeService.initializeSeats(20);
    }

    @Test
    @DisplayName("Should handle 100 concurrent reservation requests with zero double-bookings")
    void testConcurrentReservationsUnderHighLoad() throws InterruptedException {
        int totalRequests = 100;
        int totalSeats = 20;

        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(totalRequests);

        AtomicInteger successfulReservations = new AtomicInteger(0);
        AtomicInteger waitlistedUsers = new AtomicInteger(0);

        for (int i = 1; i <= totalRequests; i++) {
            final String userId = "concurrent_user_" + i;
            final int priority = (i % 3) + 1; // Priorities 1, 2, 3

            executor.submit(() -> {
                try {
                    startSignal.await(); // Wait for all threads to align
                    ReservationResponse response = ticketForgeService.reserveSeat(userId, priority);
                    if (response != null) {
                        successfulReservations.incrementAndGet();
                    } else {
                        waitlistedUsers.incrementAndGet();
                    }
                } catch (Exception ignored) {
                } finally {
                    doneSignal.countDown();
                }
            });
        }

        // Fire all 100 concurrent requests simultaneously
        startSignal.countDown();
        boolean completedInTime = doneSignal.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completedInTime).isTrue();
        assertThat(successfulReservations.get()).isEqualTo(totalSeats);
        assertThat(waitlistedUsers.get()).isEqualTo(totalRequests - totalSeats);

        SystemStatusResponse status = ticketForgeService.getSystemStatus();
        assertThat(status.totalSeats()).isEqualTo(totalSeats);
        assertThat(status.availableSeats()).isEqualTo(0);
        assertThat(status.reservedSeats()).isEqualTo(totalSeats);
        assertThat(status.waitlistCount()).isEqualTo(totalRequests - totalSeats);
    }
}
