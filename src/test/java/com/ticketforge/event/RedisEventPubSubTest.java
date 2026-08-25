package com.ticketforge.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class RedisEventPubSubTest {

    @Autowired
    private RedisEventPublisher redisEventPublisher;

    @Autowired
    private TestEventListener testEventListener;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public TestEventListener testEventListener() {
            return new TestEventListener();
        }
    }

    static class TestEventListener {
        final List<TicketForgeEvent> receivedEvents = new CopyOnWriteArrayList<>();
        volatile CountDownLatch latch = new CountDownLatch(1);

        @EventListener
        public void onEvent(TicketForgeEvent event) {
            receivedEvents.add(event);
            latch.countDown();
        }

        public void reset(int count) {
            receivedEvents.clear();
            latch = new CountDownLatch(count);
        }
    }

    @Test
    @DisplayName("Should publish domain event and receive via event listener")
    void testPublishAndReceiveEvent() throws InterruptedException {
        testEventListener.reset(1);

        TicketForgeEvent event = TicketForgeEvent.of("RESERVED", 42, "usr_pubsub_1", "Seat 42 reserved successfully");
        redisEventPublisher.publishEvent(event);

        boolean received = testEventListener.latch.await(3, TimeUnit.SECONDS);
        assertThat(received).isTrue();
        assertThat(testEventListener.receivedEvents).hasSize(1);

        TicketForgeEvent captured = testEventListener.receivedEvents.get(0);
        assertThat(captured.eventType()).isEqualTo("RESERVED");
        assertThat(captured.seatNumber()).isEqualTo(42);
        assertThat(captured.userId()).isEqualTo("usr_pubsub_1");
        assertThat(captured.message()).isEqualTo("Seat 42 reserved successfully");
        assertThat(captured.timestamp()).isNotNull();
    }
}
