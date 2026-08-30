package com.ticketforge.security;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TicketForgeUserPrincipal {
    private final String userId;
    private final String email;
    private final String role;
    private final int priorityTier;
}
