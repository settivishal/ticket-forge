package com.ticketforge.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * REST controller providing a real-time Server-Sent Events (SSE) stream.
 */
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Event Stream", description = "Server-Sent Events (SSE) real-time event broadcasting")
public class EventStreamController {

    private final SseEmitterManager sseEmitterManager;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to live event stream", description = "Establishes a persistent Server-Sent Events (SSE) stream broadcasting real-time ticketing state changes")
    public SseEmitter streamEvents() {
        log.debug("HTTP GET /api/v1/events/stream: Client connected to live SSE stream");
        return sseEmitterManager.createEmitter();
    }
}
