package com.ticketforge.graphql;

import com.ticketforge.dto.ReservationResponse;
import com.ticketforge.dto.SeatResponse;
import com.ticketforge.model.SeatStatus;
import com.ticketforge.model.SeatTier;
import com.ticketforge.security.SecurityUtils;
import com.ticketforge.service.TicketForgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SeatGraphQLController {

    private final TicketForgeService ticketForgeService;

    @QueryMapping
    public List<SeatResponse> seats(@Argument SeatStatus status, @Argument SeatTier tier) {
        log.debug("GraphQL Query: seats(status={}, tier={})", status, tier);
        List<SeatResponse> allSeats = ticketForgeService.getAllSeats();
        return allSeats.stream()
                .filter(seat -> status == null || seat.status() == status)
                .filter(seat -> tier == null || seat.tier() == tier)
                .toList();
    }

    @QueryMapping
    public SeatResponse seat(@Argument int seatNumber) {
        log.debug("GraphQL Query: seat(seatNumber={})", seatNumber);
        return ticketForgeService.getSeatByNumber(seatNumber);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ReservationResponse reserveSeat(@Argument Integer seatNumber) {
        String userId = SecurityUtils.currentUserId();
        int priority = SecurityUtils.currentPriorityTier();
        log.info("GraphQL Mutation: reserveSeat(userId={}, priority={}, seatNumber={})", userId, priority, seatNumber);
        return ticketForgeService.reserveSeat(userId, priority, seatNumber);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ReservationResponse holdSeat(@Argument Integer ttlSeconds, @Argument Integer seatNumber) {
        String userId = SecurityUtils.currentUserId();
        int priority = SecurityUtils.currentPriorityTier();
        int ttl = ttlSeconds != null && ttlSeconds > 0 ? ttlSeconds : 300;
        log.info("GraphQL Mutation: holdSeat(userId={}, priority={}, ttlSeconds={}, seatNumber={})", userId, priority, ttl, seatNumber);
        return ticketForgeService.holdSeat(userId, priority, ttl, seatNumber);
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public ReservationResponse confirmHold() {
        String userId = SecurityUtils.currentUserId();
        log.info("GraphQL Mutation: confirmHold(userId={})", userId);
        return ticketForgeService.confirmHold(userId);
    }

    @BatchMapping(typeName = "Seat", field = "occupantUserId")
    public Map<SeatResponse, String> occupantUserId(List<SeatResponse> seats) {
        return seats.stream().collect(Collectors.toMap(seat -> seat, seat -> seat.userId() != null ? seat.userId() : ""));
    }
}
