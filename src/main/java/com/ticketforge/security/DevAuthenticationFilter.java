package com.ticketforge.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Development-Only Mock Authentication Filter.
 * <p>
 * Enables instant zero-dependency authentication in Postman and local testing
 * without requiring real Supabase accounts or cloud OAuth2 JWKS validation.
 * <p>
 * Usage in dev profile:
 * - Header 'Authorization: Bearer dev-admin' (or 'X-Dev-Role: ADMIN') -> Authenticated as ROLE_ADMIN
 * - Header 'Authorization: Bearer dev-customer' (or 'X-Dev-Role: CUSTOMER') -> Authenticated as ROLE_CUSTOMER
 * - Optional 'X-Dev-User: usr_101' -> Sets custom mock userId
 * - Optional 'X-Dev-Priority: 3' -> Sets priority tier (1=Standard, 2=Premium, 3=VIP)
 */
@Component
@Profile({"dev", "test"})
@Slf4j
public class DevAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String devRoleHeader = request.getHeader("X-Dev-Role");
        String devUserHeader = request.getHeader("X-Dev-User");
        String devPriorityHeader = request.getHeader("X-Dev-Priority");

        boolean isDevAuth = false;
        String role = "ROLE_CUSTOMER";
        String userId = (devUserHeader != null && !devUserHeader.isBlank()) ? devUserHeader : "dev_customer";
        int priority = 1;

        if (devPriorityHeader != null) {
            try {
                priority = Integer.parseInt(devPriorityHeader);
            } catch (NumberFormatException ignored) {
                priority = 1;
            }
        }

        if (authHeader != null && authHeader.startsWith("Bearer dev-admin")) {
            isDevAuth = true;
            role = "ROLE_ADMIN";
            userId = (devUserHeader != null && !devUserHeader.isBlank()) ? devUserHeader : "dev_admin";
            priority = 3;
        } else if (authHeader != null && authHeader.startsWith("Bearer dev-customer")) {
            isDevAuth = true;
            role = "ROLE_CUSTOMER";
        } else if (devRoleHeader != null && !devRoleHeader.isBlank()) {
            isDevAuth = true;
            String rawRole = devRoleHeader.trim().toUpperCase();
            role = rawRole.startsWith("ROLE_") ? rawRole : "ROLE_" + rawRole;
            if ("ROLE_ADMIN".equals(role) && (devUserHeader == null || devUserHeader.isBlank())) {
                userId = "dev_admin";
                priority = 3;
            }
        }

        if (isDevAuth) {
            log.debug("DevAuthFilter: Authenticating mock user '{}' with role '{}' and priority '{}'", userId, role, priority);

            TicketForgeUserPrincipal principal = TicketForgeUserPrincipal.builder()
                    .userId(userId)
                    .email(userId + "@ticketforge.local")
                    .role(role)
                    .priorityTier(priority)
                    .build();

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority(role))
            );

            SecurityContextHolder.getContext().setAuthentication(auth);

            // Strip the mock Authorization header so downstream OAuth2 filter doesn't fail parsing it as RS256 JWT
            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if ("Authorization".equalsIgnoreCase(name) && authHeader != null && authHeader.startsWith("Bearer dev-")) {
                        return null;
                    }
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if ("Authorization".equalsIgnoreCase(name) && authHeader != null && authHeader.startsWith("Bearer dev-")) {
                        return Collections.emptyEnumeration();
                    }
                    return super.getHeaders(name);
                }
            };

            filterChain.doFilter(wrappedRequest, response);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
