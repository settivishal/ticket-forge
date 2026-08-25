package com.ticketforge.service;

import com.ticketforge.config.RedisConfig;
import com.ticketforge.dto.ReservationResponse;
import com.ticketforge.dto.SeatResponse;
import com.ticketforge.dto.SystemStatusResponse;
import com.ticketforge.dto.WaitlistResponse;
import com.ticketforge.model.SeatStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class RedisCacheIntegrationTest {

    @Autowired
    private TicketForgeService ticketForgeService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Clear all caches before each test
        clearAllCaches();
        ticketForgeService.initializeSeats(10);
    }

    private void clearAllCaches() {
        for (String cacheName : List.of(
                RedisConfig.CACHE_SYSTEM_STATUS,
                RedisConfig.CACHE_SEATS,
                RedisConfig.CACHE_SEAT,
                RedisConfig.CACHE_WAITLIST,
                RedisConfig.CACHE_RESERVATIONS)) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    @Test
    @DisplayName("Should populate and evict system status cache on state mutation")
    void testSystemStatusCache() {
        Cache statusCache = cacheManager.getCache(RedisConfig.CACHE_SYSTEM_STATUS);
        assertThat(statusCache).isNotNull();

        // 1. Initial call should populate the cache
        SystemStatusResponse status1 = ticketForgeService.getSystemStatus();
        assertThat(status1.totalSeats()).isEqualTo(10);
        assertThat(status1.availableSeats()).isEqualTo(10);

        // Verify cache entry exists
        Cache.ValueWrapper cachedWrapper = statusCache.get("status");
        assertThat(cachedWrapper).isNotNull();
        assertThat(cachedWrapper.get()).isEqualTo(status1);

        // 2. Reserve a seat - should trigger @CacheEvict
        ticketForgeService.reserveSeat("usr_cache_1", 1);

        // Verify cache entry was evicted
        assertThat(statusCache.get("status")).isNull();

        // 3. Next read should re-populate cache with updated status
        SystemStatusResponse status2 = ticketForgeService.getSystemStatus();
        assertThat(status2.availableSeats()).isEqualTo(9);
        assertThat(status2.reservedSeats()).isEqualTo(1);

        assertThat(statusCache.get("status")).isNotNull();
    }

    @Test
    @DisplayName("Should populate and evict seat list and single seat caches on reservation")
    void testSeatCacheEviction() {
        Cache seatsCache = cacheManager.getCache(RedisConfig.CACHE_SEATS);
        Cache singleSeatCache = cacheManager.getCache(RedisConfig.CACHE_SEAT);
        assertThat(seatsCache).isNotNull();
        assertThat(singleSeatCache).isNotNull();

        // 1. Query seats and individual seat
        List<SeatResponse> allSeats = ticketForgeService.getAllSeats();
        SeatResponse seat1 = ticketForgeService.getSeatByNumber(1);
        assertThat(allSeats).hasSize(10);
        assertThat(seat1.status()).isEqualTo(SeatStatus.AVAILABLE);

        // Verify cached
        assertThat(seatsCache.get("all")).isNotNull();
        assertThat(singleSeatCache.get(1)).isNotNull();

        // 2. Reserve seat 1
        ReservationResponse res = ticketForgeService.reserveSeat("usr_seat_cached", 1);
        assertThat(res.seatNumber()).isEqualTo(1);

        // Verify both caches were evicted
        assertThat(seatsCache.get("all")).isNull();
        assertThat(singleSeatCache.get(1)).isNull();

        // 3. Re-query shows RESERVED status and re-caches
        SeatResponse updatedSeat1 = ticketForgeService.getSeatByNumber(1);
        assertThat(updatedSeat1.status()).isEqualTo(SeatStatus.RESERVED);
        assertThat(updatedSeat1.userId()).isEqualTo("usr_seat_cached");
    }

    @Test
    @DisplayName("Should cache waitlist and evict upon waitlist mutations and cancellations")
    void testWaitlistCache() {
        Cache waitlistCache = cacheManager.getCache(RedisConfig.CACHE_WAITLIST);
        assertThat(waitlistCache).isNotNull();

        // Book all 10 seats
        for (int i = 1; i <= 10; i++) {
            ticketForgeService.reserveSeat("usr_" + i, 1);
        }

        // Add 2 users to waitlist
        ticketForgeService.reserveSeat("waiter_1", 2);
        ticketForgeService.reserveSeat("waiter_2", 3);

        // 1. Query waitlist -> populates cache
        List<WaitlistResponse> waitlist = ticketForgeService.getWaitlist();
        assertThat(waitlist).hasSize(2);
        assertThat(waitlistCache.get("queue")).isNotNull();

        // 2. Update priority -> evicts waitlist cache
        ticketForgeService.updatePriority("waiter_1", 5);
        assertThat(waitlistCache.get("queue")).isNull();

        // 3. Cancel reservation on seat 1 -> triggers waitlist auto-promotion & eviction
        ticketForgeService.cancelReservation(1, "usr_1");
        assertThat(waitlistCache.get("queue")).isNull();

        // 4. Verify waitlist updated (waiter_1 with priority 5 was promoted, remaining size = 1)
        List<WaitlistResponse> updatedWaitlist = ticketForgeService.getWaitlist();
        assertThat(updatedWaitlist).hasSize(1);
        assertThat(updatedWaitlist.get(0).userId()).isEqualTo("waiter_2");
    }
}
