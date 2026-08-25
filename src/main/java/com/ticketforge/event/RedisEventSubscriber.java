package com.ticketforge.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Cross-Node Event Subscriber.
 * Listens for events published to Redis topic 'ticketforge:events' by other backend instances
 * and bridges them to the local Spring event pipeline for SSE/GraphQL delivery.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisEventSubscriber implements MessageListener {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper redisObjectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            byte[] body = message.getBody();
            TicketForgeEvent event = redisObjectMapper.readValue(body, TicketForgeEvent.class);
            log.debug("Received event from Redis Pub/Sub: {}", event);
            applicationEventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to deserialize Redis Pub/Sub message: {}", e.getMessage());
        }
    }
}
