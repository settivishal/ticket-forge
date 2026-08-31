package com.ticketforge.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Accessors for the authenticated caller.
 * <p>
 * Booking operations must derive the acting user from the security context rather than
 * from request parameters. Trusting a client-supplied user id would let any authenticated
 * caller act as anybody else — reserving, cancelling or re-prioritising on their behalf.
 */
public final class SecurityUtils {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private SecurityUtils() {
    }

    /**
     * Returns the authenticated principal, or null when the request is anonymous.
     */
    public static TicketForgeUserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return (principal instanceof TicketForgeUserPrincipal p) ? p : null;
    }

    /**
     * Returns the authenticated caller's user id.
     * <p>
     * Falls back to the authentication name when the principal is not a
     * {@link TicketForgeUserPrincipal}, so the identity of callers authenticated by other
     * means (for example plain username/password tokens) still resolves.
     *
     * @throws IllegalStateException if there is no authenticated principal. Endpoints
     *                               reaching this point are already behind an
     *                               {@code authenticated()} matcher, so this indicates a
     *                               misconfigured filter chain rather than a client error.
     */
    public static String currentUserId() {
        TicketForgeUserPrincipal principal = currentPrincipal();
        if (principal != null && principal.getUserId() != null) {
            return principal.getUserId();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null) {
            return authentication.getName();
        }
        throw new IllegalStateException("No authenticated user present in the security context");
    }

    /**
     * Returns the caller's priority tier, defaulting to standard (1) when unavailable.
     * Priority is an attribute of the account, never of the request body.
     */
    public static int currentPriorityTier() {
        TicketForgeUserPrincipal principal = currentPrincipal();
        if (principal == null || principal.getPriorityTier() < 1) {
            return 1;
        }
        return principal.getPriorityTier();
    }

    public static boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (ROLE_ADMIN.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
