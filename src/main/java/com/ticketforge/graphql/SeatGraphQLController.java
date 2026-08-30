package com.ticketforge.graphql;

import com.ticketforge.dto.ReservationResponse;
import com.ticketforge.dto.SeatResponse;
import com.ticketforge.model.SeatStatus;
import com.ticketforge.model.SeatTier;
import com.ticketforge.service.TicketForgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
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
    public ReservationResponse reserveSeat(@Argument String userId, @Argument int priority) {
        log.info("GraphQL Mutation: reserveSeat(userId={}, priority={})", userId, priority);
        return ticketForgeService.reserveSeat(userId, priority);
    }

    @MutationMapping
    public ReservationResponse holdSeat(@Argument String userId, @Argument int priority, @Argument Integer ttlSeconds) {
        int ttl = ttlSeconds != null && ttlSeconds > 0 ? ttlSeconds : 300;
        log.info("GraphQL Mutation: holdSeat(userId={}, priority={}, ttlSeconds={})", userId, priority, ttl);
        return ticketForgeService.holdSeat(userId, priority, ttl);
    }

    @BatchMapping(typeName = "Seat", field = "occupantUserId")
    public Map<SeatResponse, String> occupantUserId(List<SeatResponse> seats) {
        return seats.stream().collect(Collectors.toMap(seat -> seat, seat -> seat.userId() != null ? seat.userId() : ""));
    }
}
