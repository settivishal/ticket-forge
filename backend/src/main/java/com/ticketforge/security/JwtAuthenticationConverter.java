package com.ticketforge.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String userId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");

        // Extract role from Supabase app_metadata (admin-controlled)
        String role = "ROLE_CUSTOMER";
        Map<String, Object> appMetadata = jwt.getClaim("app_metadata");
        if (appMetadata != null && appMetadata.containsKey("role")) {
            String rawRole = String.valueOf(appMetadata.get("role")).toUpperCase();
            role = rawRole.startsWith("ROLE_") ? rawRole : "ROLE_" + rawRole;
        }

        // Extract priority_tier from app_metadata (default to 1). Never user_metadata:
        // Supabase lets users edit their own user_metadata, which would let anyone jump the waitlist.
        int priorityTier = 1;
        if (appMetadata != null && appMetadata.containsKey("priority_tier")) {
            try {
                priorityTier = Integer.parseInt(String.valueOf(appMetadata.get("priority_tier")));
            } catch (NumberFormatException ignored) {
                priorityTier = 1;
            }
        }
        priorityTier = Math.max(1, Math.min(5, priorityTier));

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(role));

        TicketForgeUserPrincipal principal = TicketForgeUserPrincipal.builder()
                .userId(userId)
                .email(email)
                .role(role)
                .priorityTier(priorityTier)
                .build();

        return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }
}
