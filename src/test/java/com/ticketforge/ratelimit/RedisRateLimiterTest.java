package com.ticketforge.ratelimit;

import com.ticketforge.exception.RateLimitExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
class RedisRateLimiterTest {

    @Autowired
    private RedisRateLimiterService rateLimiterService;

    @Test
    @DisplayName("Should allow requests within configured token rate limit")
    void testAllowRequestsWithinRateLimit() {
        String testUser = "usr_ratelimit_allow";

        // Default limit is 5 req/sec: first 5 should succeed
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiterService.tryAcquire(testUser)).isTrue();
        }
    }

    @Test
    @DisplayName("Should throttle requests and reject when burst limit is exceeded")
    void testThrottleRequestsExceedingLimit() {
        String testUser = "usr_ratelimit_burst";

        // Consume all 5 tokens
        for (int i = 0; i < 5; i++) {
            rateLimiterService.tryAcquire(testUser);
        }

        // 6th request should fail
        assertThat(rateLimiterService.tryAcquire(testUser)).isFalse();

        // checkRateLimit should throw RateLimitExceededException
        assertThatThrownBy(() -> rateLimiterService.checkRateLimit(testUser))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Rate limit exceeded for user/IP");
    }

    @Test
    @DisplayName("Should allow requests with null or blank key without throttling")
    void testNullOrBlankKey() {
        assertThat(rateLimiterService.tryAcquire(null)).isTrue();
        assertThat(rateLimiterService.tryAcquire("")).isTrue();
    }
}
