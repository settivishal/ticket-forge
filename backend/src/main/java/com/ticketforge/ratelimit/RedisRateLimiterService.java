package com.ticketforge.ratelimit;

import com.ticketforge.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enterprise Token-Bucket Rate Limiter.
 * Protects booking endpoints against automated ticket scalping bots and DDoS bursts.
 * Uses Redisson RRateLimiter for cluster-wide rate enforcement, with in-memory token bucket fallback.
 */
@Service
@Slf4j
public class RedisRateLimiterService {

    public static final long DEFAULT_RATE_PER_SECOND = 5;

    private final RedissonClient redissonClient;
    private final ConcurrentHashMap<String, LocalTokenBucket> localBuckets = new ConcurrentHashMap<>();

    public RedisRateLimiterService(@Autowired(required = false) RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    /**
     * Attempts to acquire 1 permit under the default rate limit (5 req/sec).
     *
     * @param key Client identifier (e.g., user ID or IP address)
     * @return true if permit was acquired, false if rate limited
     */
    public boolean tryAcquire(String key) {
        return tryAcquire(key, DEFAULT_RATE_PER_SECOND, 1);
    }

    /**
     * Attempts to acquire permits under a specified rate limit.
     *
     * @param key           Client identifier
     * @param ratePerSecond Maximum allowed requests per second
     * @param permits       Number of permits requested
     * @return true if permit was acquired, false if rate limited
     */
    public boolean tryAcquire(String key, long ratePerSecond, int permits) {
        if (key == null || key.isBlank()) {
            return true;
        }

        if (redissonClient != null) {
            return tryAcquireDistributed(key, ratePerSecond, permits);
        } else {
            return tryAcquireLocal(key, ratePerSecond, permits);
        }
    }

    /**
     * Enforces rate limiting, throwing RateLimitExceededException if rate is exceeded.
     */
    public void checkRateLimit(String key) {
        if (!tryAcquire(key)) {
            log.warn("Rate limit exceeded for key: {}", key);
            throw new RateLimitExceededException("Rate limit exceeded for user/IP: " + key + ". Max 5 requests per second allowed.");
        }
    }

    private boolean tryAcquireDistributed(String key, long ratePerSecond, int permits) {
        try {
            RRateLimiter rateLimiter = redissonClient.getRateLimiter("ratelimit:" + key);
            rateLimiter.trySetRate(RateType.OVERALL, ratePerSecond, 1, RateIntervalUnit.SECONDS);
            return rateLimiter.tryAcquire(permits);
        } catch (Exception e) {
            log.warn("Distributed rate limiter error for '{}', falling back to local limiter: {}", key, e.getMessage());
            return tryAcquireLocal(key, ratePerSecond, permits);
        }
    }

    private boolean tryAcquireLocal(String key, long ratePerSecond, int permits) {
        LocalTokenBucket bucket = localBuckets.computeIfAbsent(key, k -> new LocalTokenBucket(ratePerSecond));
        return bucket.tryConsume(permits);
    }

    private static class LocalTokenBucket {
        private final long capacity;
        private final AtomicInteger tokens;
        private volatile long lastRefillTimestamp;

        public LocalTokenBucket(long capacity) {
            this.capacity = capacity;
            this.tokens = new AtomicInteger((int) capacity);
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume(int permits) {
            refill();
            int current = tokens.get();
            if (current >= permits) {
                tokens.addAndGet(-permits);
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsedSeconds = (now - lastRefillTimestamp) / 1000;
            if (elapsedSeconds > 0) {
                int tokensToAdd = (int) (elapsedSeconds * capacity);
                tokens.set(Math.min((int) capacity, tokens.get() + tokensToAdd));
                lastRefillTimestamp = now;
            }
        }
    }
}
