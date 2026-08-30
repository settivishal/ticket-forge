package com.ticketforge.controller;

import com.ticketforge.dto.ApiResponse;
import com.ticketforge.event.TicketForgeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe manager for Server-Sent Events (SSE) client connections.
 * Handles client registration, event broadcasting, keep-alive heartbeats,
 * and graceful lifecycle cleanup.
 */
@Component
@Slf4j
public class SseEmitterManager {

    private static final long DEFAULT_TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes
    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();

    /**
     * Registers and initializes a new SSE client emitter.
     */
    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MS);
        emitters.add(emitter);

        emitter.onCompletion(() -> {
            log.debug("SSE emitter completed. Active clients: {}", emitters.size() - 1);
            emitters.remove(emitter);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE emitter timed out. Active clients: {}", emitters.size() - 1);
            emitter.complete();
            emitters.remove(emitter);
        });

        emitter.onError(throwable -> {
            log.debug("SSE emitter error: {}. Active clients: {}", throwable.getMessage(), emitters.size() - 1);
            emitter.complete();
            emitters.remove(emitter);
        });

        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .id("0")
                    .data(ApiResponse.success("Connected to TicketForge Live SSE Stream")));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE INIT event: {}", e.getMessage());
            emitters.remove(emitter);
        }

        log.info("Registered new SSE client connection. Total active clients: {}", emitters.size());
        return emitter;
    }

    /**
     * Broadcasts a domain event to all active SSE client streams.
     */
    public void broadcast(TicketForgeEvent event) {
        if (emitters.isEmpty()) {
            return;
        }

        log.debug("Broadcasting SSE event '{}' to {} clients", event.eventType(), emitters.size());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.eventType())
                        .id(String.valueOf(event.timestamp().toEpochMilli()))
                        .data(event));
            } catch (Exception e) {
                log.debug("Error sending SSE event to client: {}. Removing emitter.", e.getMessage());
                emitter.complete();
                emitters.remove(emitter);
            }
        }
    }

    /**
     * Listens to internal Spring domain events published across the application.
     */
    @EventListener
    public void handleTicketForgeEvent(TicketForgeEvent event) {
        broadcast(event);
    }

    /**
     * Periodic 15-second heartbeat ping to prevent intermediate proxies/firewalls
     * from timing out idle connections.
     */
    @Scheduled(fixedRate = 15000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (Exception e) {
                emitter.complete();
                emitters.remove(emitter);
            }
        }
    }

    /**
     * Returns the number of currently active client connections.
     */
    public int getActiveEmitterCount() {
        return emitters.size();
    }
}
