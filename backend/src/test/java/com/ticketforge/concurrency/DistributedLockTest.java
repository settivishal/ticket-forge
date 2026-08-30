package com.ticketforge.concurrency;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
class DistributedLockTest {

    @Autowired
    private DistributedLockManager distributedLockManager;

    private AtomicInteger sharedCounter;

    @BeforeEach
    void setUp() {
        sharedCounter = new AtomicInteger(0);
    }

    @Test
    @DisplayName("Should execute supplier and return value under distributed lock")
    void testExecuteWithLockSupplier() {
        String result = distributedLockManager.executeWithLock("test:lock:1", 5, 10, () -> "SUCCESS");
        assertThat(result).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("Should execute runnable action under distributed lock")
    void testExecuteWithLockRunnable() {
        distributedLockManager.executeWithLock("test:lock:runnable", 5, 10, () -> sharedCounter.incrementAndGet());
        assertThat(sharedCounter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should guarantee mutual exclusion for 20 concurrent threads incrementing shared state")
    void testConcurrentLockExecution() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        String lockKey = DistributedLockManager.seatLockKey(42);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    distributedLockManager.executeWithLock(lockKey, 10, 15, () -> {
                        int current = sharedCounter.get();
                        try {
                            Thread.sleep(5); // Simulate critical section work
                        } catch (InterruptedException ignored) {}
                        sharedCounter.set(current + 1);
                    });
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(sharedCounter.get()).isEqualTo(threadCount);
    }
}
