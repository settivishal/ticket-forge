package com.ticketforge.cache;

import com.ticketforge.config.RedisConfig;
import com.ticketforge.dto.ReservationResponse;
import com.ticketforge.dto.SystemStatusResponse;
import com.ticketforge.event.RedisEventPublisher;
import com.ticketforge.event.TicketForgeEvent;
import com.ticketforge.ratelimit.RedisRateLimiterService;
import com.ticketforge.service.TicketForgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for Redis Caching, Token Bucket Rate Limiter, and Event Pub/Sub.
 */
@SpringBootTest
@ActiveProfiles("dev")
class RedisCacheAndRateLimitingIntegrationTest {

    @Autowired
    private TicketForgeService ticketForgeService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisRateLimiterService rateLimiterService;

    @Autowired
    private RedisEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        ticketForgeService.initializeSeats(10);
    }

    @Test
    @DisplayName("Cache-Aside Eviction: Booking and Cancelling evicts cached aggregates")
    void testCacheAsideEvictionLifecycle() {
        Cache statusCache = cacheManager.getCache(RedisConfig.CACHE_SYSTEM_STATUS);
        assertThat(statusCache).isNotNull();

        // 1. First call populates cache
        SystemStatusResponse status1 = ticketForgeService.getSystemStatus();
        assertThat(status1.availableSeats()).isEqualTo(10);

        // 2. Booking a seat executes @CacheEvict
        ReservationResponse res = ticketForgeService.reserveSeat("usr_cache_test", 1);
        assertThat(res).isNotNull();

        // 3. Next call retrieves fresh state with 9 available seats
        SystemStatusResponse status2 = ticketForgeService.getSystemStatus();
        assertThat(status2.availableSeats()).isEqualTo(9);
        assertThat(status2.reservedSeats()).isEqualTo(1);
    }

    @Test
    @DisplayName("Rate Limiter: Enforces Token Bucket capacity and rejects bursts exceeding rate")
    void testRateLimiterRejection() {
        String clientKey = "client_bot_ip_192_168_1_50";
        long ratePerSecond = 5;

        // First 5 permits should succeed
        for (int i = 0; i < 5; i++) {
            boolean acquired = rateLimiterService.tryAcquire(clientKey, ratePerSecond, 1);
            assertThat(acquired).isTrue();
        }

        // 6th permit within the same second must be rejected (rate limited)
        boolean excessAcquired = rateLimiterService.tryAcquire(clientKey, ratePerSecond, 1);
        assertThat(excessAcquired).isFalse();
    }

    @Test
    @DisplayName("Event Pub/Sub: Local Domain Event Publishing operates without crashing when Redis is local fallback")
    void testLocalEventPublishing() {
        TicketForgeEvent event = TicketForgeEvent.of("RESERVED", 7, "usr_pubsub_test", "Seat 7 reserved");
        eventPublisher.publishEvent(event);
        assertThat(event.eventType()).isEqualTo("RESERVED");
        assertThat(event.seatNumber()).isEqualTo(7);
    }
}
