package com.ticketforge.controller;

import com.ticketforge.event.TicketForgeEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class EventStreamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SseEmitterManager sseEmitterManager;

    @Test
    @DisplayName("GET /api/v1/events/stream - establishes SSE connection returning text/event-stream")
    void testEventStreamConnection() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/events/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentType()).contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    @Test
    @DisplayName("SseEmitterManager broadcasts events to connected clients")
    void testSseEmitterManagerBroadcast() {
        int initialCount = sseEmitterManager.getActiveEmitterCount();
        SseEmitter emitter = sseEmitterManager.createEmitter();

        assertThat(sseEmitterManager.getActiveEmitterCount()).isGreaterThan(initialCount);

        TicketForgeEvent event = TicketForgeEvent.of("RESERVED", 1, "usr_sse_test", "Test seat reserved");
        sseEmitterManager.broadcast(event);

        emitter.complete();
    }
}
