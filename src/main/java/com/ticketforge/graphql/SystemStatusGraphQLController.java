package com.ticketforge.graphql;

import com.ticketforge.dto.SystemStatusResponse;
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
public class SystemStatusGraphQLController {

    private final TicketForgeService ticketForgeService;

    @QueryMapping
    public SystemStatusResponse systemStatus() {
        log.debug("GraphQL Query: systemStatus");
        return ticketForgeService.getSystemStatus();
    }

    @MutationMapping
    public SystemStatusResponse initializeSeats(@Argument int count) {
        log.info("GraphQL Mutation: initializeSeats(count={})", count);
        ticketForgeService.initializeSeats(count);
        return ticketForgeService.getSystemStatus();
    }

    @MutationMapping
    public SystemStatusResponse addSeats(@Argument int count) {
        log.info("GraphQL Mutation: addSeats(count={})", count);
        ticketForgeService.addSeats(count);
        return ticketForgeService.getSystemStatus();
    }

    @MutationMapping
    public List<Integer> releaseSeats(@Argument String fromUserId, @Argument String toUserId) {
        log.info("GraphQL Mutation: releaseSeats(fromUserId={}, toUserId={})", fromUserId, toUserId);
        return ticketForgeService.releaseSeats(fromUserId, toUserId);
    }
}
