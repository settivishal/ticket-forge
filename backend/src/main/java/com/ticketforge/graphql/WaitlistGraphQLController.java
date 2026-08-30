package com.ticketforge.graphql;

import com.ticketforge.dto.WaitlistResponse;
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
public class WaitlistGraphQLController {

    private final TicketForgeService ticketForgeService;

    @QueryMapping
    public List<WaitlistResponse> waitlist() {
        log.debug("GraphQL Query: waitlist");
        return ticketForgeService.getWaitlist();
    }

    @MutationMapping
    public boolean exitWaitlist(@Argument String userId) {
        log.info("GraphQL Mutation: exitWaitlist(userId={})", userId);
        return ticketForgeService.exitWaitlist(userId);
    }

    @MutationMapping
    public boolean updatePriority(@Argument String userId, @Argument int priority) {
        log.info("GraphQL Mutation: updatePriority(userId={}, priority={})", userId, priority);
        return ticketForgeService.updatePriority(userId, priority);
    }
}
