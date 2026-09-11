package com.aibackend.AiBasedEndtoEndSystem.controller;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.service.SupabaseTokenService;
import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Issues short-lived Supabase Realtime JWTs for the authenticated user.
 *
 * The frontend calls this endpoint once on login (and refreshes before expiry)
 * to get a JWT it can pass to the Supabase client for real-time subscriptions.
 * The JWT embeds the user's MongoDB ID as 'mongodb_user_id', which is used
 * by Supabase RLS policies to filter the notifications table.
 *
 * Security: requires a valid Spring Security JWT (Authorization: Bearer {token}).
 * The Supabase JWT is signed with SUPABASE_JWT_SECRET (not the Spring JWT secret).
 */
@RestController
@RequestMapping("/realtime")
@Slf4j
public class RealtimeTokenController {

    @Autowired
    private SupabaseTokenService supabaseTokenService;

    /**
     * GET /api/realtime/supabase-token
     *
     * Returns a short-lived Supabase JWT for Realtime subscription authentication.
     *
     * Response body:
     * {
     *   "token": "eyJ...",
     *   "expiresAt": "2024-01-01T12:00:00Z",
     *   "ttlSeconds": 3600,
     *   "userId": "c-0001"
     * }
     */
    @GetMapping("/supabase-token")
    public ResponseEntity<?> getSupabaseToken() {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null) {
            log.warn("Supabase token request rejected: no authenticated user in SecurityContext");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }

        String token = supabaseTokenService.generateRealtimeToken(user);
        if (token == null) {
            log.error("Failed to generate Supabase Realtime token for user={}", user.getId());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Supabase JWT secret not configured. Contact administrator."));
        }

        long ttlSeconds = supabaseTokenService.getTokenTtlSeconds();
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("expiresAt", Instant.now().plusSeconds(ttlSeconds).toString());
        response.put("ttlSeconds", ttlSeconds);
        response.put("userId", user.getId());

        log.debug("Supabase Realtime token issued for user={}, role={}", user.getId(), user.getRole());
        return ResponseEntity.ok(response);
    }
}
