package com.ticketforge.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Cross-Node Event Publisher.
 * Publishes domain events to Redis Pub/Sub topic 'ticketforge:events' to synchronize
 * real-time SSE streams and GraphQL subscriptions across clustered backend instances.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisEventPublisher {

    public static final String EVENT_TOPIC = "ticketforge:events";

    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Broadcasts an event locally to the Spring application context and across the Redis cluster.
     *
     * @param event The domain event to publish
     */
    public void publishEvent(TicketForgeEvent event) {
        log.debug("Publishing local domain event: {}", event);
        applicationEventPublisher.publishEvent(event);

        if (redisTemplate != null) {
            try {
                redisTemplate.convertAndSend(EVENT_TOPIC, event);
                log.debug("Published event to Redis topic '{}': {}", EVENT_TOPIC, event);
            } catch (Exception e) {
                log.warn("Failed to broadcast event to Redis topic '{}': {}", EVENT_TOPIC, e.getMessage());
            }
        }
    }
}
