package com.ticketforge.ratelimit;

import com.ticketforge.security.SecurityUtils;
import com.ticketforge.security.TicketForgeUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Token-bucket rate limiting for mutating API calls (reserve, hold, cancel, waitlist changes).
 * <p>
 * Runs after authentication so the bucket is keyed by user id; anonymous callers are keyed
 * by remote address. Read endpoints are cached and left unlimited.
 */
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final RedisRateLimiterService rateLimiterService;
    private final long ratePerSecond;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!LIMITED_METHODS.contains(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !(path.startsWith("/api/") || path.startsWith("/graphql"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = clientKey(request);
        if (rateLimiterService.tryAcquire(key, ratePerSecond, 1)) {
            chain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit exceeded for '{}' on {} {}", key, request.getMethod(), request.getRequestURI());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader("Retry-After", "1");
        response.getWriter().write("{\"type\":\"https://ticketforge.com/errors/rate-limit-exceeded\","
                + "\"title\":\"Rate Limit Exceeded\",\"status\":429,"
                + "\"detail\":\"Max " + ratePerSecond + " write requests per second allowed.\"}");
    }

    private static String clientKey(HttpServletRequest request) {
        TicketForgeUserPrincipal principal = SecurityUtils.currentPrincipal();
        if (principal != null && principal.getUserId() != null) {
            return "user:" + principal.getUserId();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
            return "user:" + auth.getName();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
