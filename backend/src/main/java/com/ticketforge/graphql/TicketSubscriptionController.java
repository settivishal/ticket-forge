package com.ticketforge.graphql;

import com.ticketforge.dto.SystemStatusResponse;
import com.ticketforge.event.TicketForgeEvent;
import com.ticketforge.service.TicketForgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Controller
@RequiredArgsConstructor
@Slf4j
public class TicketSubscriptionController {

    private final TicketForgeService ticketForgeService;

    private final Sinks.Many<TicketForgeEvent> seatEventSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<SystemStatusResponse> statusSink = Sinks.many().multicast().onBackpressureBuffer();

    @SubscriptionMapping
    public Flux<TicketForgeEvent> seatEvents() {
        log.info("Client subscribed to GraphQL Subscription: seatEvents");
        return seatEventSink.asFlux();
    }

    @SubscriptionMapping
    public Flux<SystemStatusResponse> systemStatusUpdates() {
        log.info("Client subscribed to GraphQL Subscription: systemStatusUpdates");
        return statusSink.asFlux();
    }

    @EventListener
    public void handleTicketForgeEvent(TicketForgeEvent event) {
        log.debug("Broadcasting TicketForgeEvent to GraphQL subscribers: {}", event);
        seatEventSink.tryEmitNext(event);
        try {
            SystemStatusResponse currentStatus = ticketForgeService.getSystemStatus();
            statusSink.tryEmitNext(currentStatus);
        } catch (Exception e) {
            log.trace("Suppressed status update during event broadcast: {}", e.getMessage());
        }
    }
}
