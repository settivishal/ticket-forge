package com.ticketforge.controller;

import com.ticketforge.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;

/**
 * Public endpoint exposing client authentication configuration (Supabase URL & Anon Key or Dev mode flag).
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication Configuration", description = "Public client authentication metadata for Supabase & Dev profiles")
public class AuthConfigController {

    private final Environment environment;

    @Value("${supabase.project-id:${SUPABASE_PROJECT_ID:mksdjnpmljjjrevywutt}}")
    private String supabaseProjectId;

    @Value("${supabase.url:${SUPABASE_URL:https://${SUPABASE_PROJECT_ID:mksdjnpmljjjrevywutt}.supabase.co}}")
    private String supabaseUrl;

    @Value("${supabase.anon-key:${SUPABASE_ANON_KEY:}}")
    private String supabaseAnonKey;

    @GetMapping("/config")
    @Operation(summary = "Get client auth configuration", description = "Returns public client authentication metadata for Supabase Auth in staging/prod or local mock dev flag")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuthConfig() {
        boolean isDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");

        Map<String, Object> config = Map.of(
                "isDev", isDev,
                "supabaseProjectId", supabaseProjectId,
                "supabaseUrl", supabaseUrl,
                "supabaseAnonKey", supabaseAnonKey
        );

        return ResponseEntity.ok(ApiResponse.success("Authentication configuration retrieved", config));
    }
}
