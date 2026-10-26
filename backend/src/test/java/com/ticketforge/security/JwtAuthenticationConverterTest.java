package com.ticketforge.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationConverterTest {

    private final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

    private TicketForgeUserPrincipal convert(Map<String, Object> appMetadata, Map<String, Object> userMetadata) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-1")
                .claim("app_metadata", appMetadata)
                .claim("user_metadata", userMetadata)
                .build();
        return (TicketForgeUserPrincipal) converter.convert(jwt).getPrincipal();
    }

    @Test
    @DisplayName("priority_tier in user-editable user_metadata is ignored")
    void ignoresUserMetadataPriority() {
        assertThat(convert(Map.of(), Map.of("priority_tier", 5)).getPriorityTier()).isEqualTo(1);
    }

    @Test
    @DisplayName("priority_tier is read from admin-controlled app_metadata and clamped to 1..5")
    void readsAppMetadataPriority() {
        assertThat(convert(Map.of("priority_tier", 3), Map.of()).getPriorityTier()).isEqualTo(3);
        assertThat(convert(Map.of("priority_tier", 99), Map.of()).getPriorityTier()).isEqualTo(5);
    }
}
