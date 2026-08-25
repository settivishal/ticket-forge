package com.ticketforge.graphql;

import com.ticketforge.dto.ReservationResponse;
import com.ticketforge.service.TicketForgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ReservationGraphQLController {

    private final TicketForgeService ticketForgeService;

    @QueryMapping
    public List<ReservationResponse> reservations() {
        log.debug("GraphQL Query: reservations");
        return ticketForgeService.getAllReservations();
    }

    @QueryMapping
    public ReservationResponse reservation(@Argument String userId) {
        log.debug("GraphQL Query: reservation(userId={})", userId);
        return ticketForgeService.getReservationByUserId(userId);
    }

    @MutationMapping
    public boolean cancelReservation(@Argument int seatNumber, @Argument String userId) {
        log.info("GraphQL Mutation: cancelReservation(seatNumber={}, userId={})", seatNumber, userId);
        ticketForgeService.cancelReservation(seatNumber, userId);
        return true;
    }
}
