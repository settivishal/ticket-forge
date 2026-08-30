package com.ticketforge.concurrency;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Enterprise Distributed Lock Manager.
 * Uses Redisson RLock for cluster-wide mutual exclusion across multiple instances.
 * Falls back to thread-safe local ReentrantLock if Redis is unavailable or offline.
 */
@Component
@Slf4j
public class DistributedLockManager {

    private final RedissonClient redissonClient;
    private final ConcurrentHashMap<String, ReentrantLock> localLockMap = new ConcurrentHashMap<>();

    public DistributedLockManager(@Autowired(required = false) RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
        if (redissonClient != null) {
            log.info("DistributedLockManager initialized with Redisson distributed locking (cluster-ready)");
        } else {
            log.info("DistributedLockManager initialized with local JVM ReentrantLock fallback");
        }
    }

    public static String seatLockKey(int seatNumber) {
        return "lock:seat:" + seatNumber;
    }

    public static String inventoryLockKey() {
        return "lock:inventory";
    }

    public static String userLockKey(String userId) {
        return "lock:user:" + userId;
    }

    /**
     * Executes a task under a distributed lock.
     *
     * @param lockKey         Unique lock name
     * @param waitTimeSeconds Maximum time to wait for lock acquisition
     * @param leaseTimeSeconds Maximum time lock is held before auto-release
     * @param task            The task to execute
     * @return Result of the task execution
     */
    public <T> T executeWithLock(String lockKey, long waitTimeSeconds, long leaseTimeSeconds, Supplier<T> task) {
        if (redissonClient != null) {
            return executeWithRedissonLock(lockKey, waitTimeSeconds, leaseTimeSeconds, task);
        } else {
            return executeWithLocalLock(lockKey, waitTimeSeconds, task);
        }
    }

    /**
     * Executes a runnable action under a distributed lock.
     */
    public void executeWithLock(String lockKey, long waitTimeSeconds, long leaseTimeSeconds, Runnable action) {
        executeWithLock(lockKey, waitTimeSeconds, leaseTimeSeconds, () -> {
            action.run();
            return null;
        });
    }

    private <T> T executeWithRedissonLock(String lockKey, long waitTimeSeconds, long leaseTimeSeconds, Supplier<T> task) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTimeSeconds, leaseTimeSeconds, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Failed to acquire distributed lock on '{}' within {}s", lockKey, waitTimeSeconds);
                throw new IllegalStateException("Could not acquire lock for key: " + lockKey);
            }
            log.debug("Acquired distributed lock: {}", lockKey);
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrupted while waiting for lock: " + lockKey, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                    log.debug("Released distributed lock: {}", lockKey);
                } catch (Exception e) {
                    log.warn("Error unlocking distributed lock '{}': {}", lockKey, e.getMessage());
                }
            }
        }
    }

    private <T> T executeWithLocalLock(String lockKey, long waitTimeSeconds, Supplier<T> task) {
        ReentrantLock lock = localLockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitTimeSeconds, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Failed to acquire local lock on '{}' within {}s", lockKey, waitTimeSeconds);
                throw new IllegalStateException("Could not acquire lock for key: " + lockKey);
            }
            log.debug("Acquired local lock: {}", lockKey);
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrupted while waiting for lock: " + lockKey, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Released local lock: {}", lockKey);
            }
        }
    }

    public boolean isDistributedLockingActive() {
        return redissonClient != null;
    }
}
