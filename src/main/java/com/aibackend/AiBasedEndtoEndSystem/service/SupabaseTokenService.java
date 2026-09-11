package com.aibackend.AiBasedEndtoEndSystem.service;

import com.aibackend.AiBasedEndtoEndSystem.config.SupabaseConfig;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Issues short-lived Supabase-compatible JWTs for frontend Realtime authentication.
 *
 * Architecture:
 *  - Users are not in Supabase Auth — Spring Security + JWT is authoritative.
 *  - The frontend needs a token to authenticate Supabase Realtime subscriptions
 *    (for RLS-filtered Postgres Changes on the notifications table).
 *  - This service signs a JWT with SUPABASE_JWT_SECRET (same secret Supabase uses)
 *    containing a custom claim 'mongodb_user_id' = the user's MongoDB ID.
 *  - RLS policies check this claim to filter notifications per user.
 *
 * Token claims:
 *  {
 *    "role": "authenticated",
 *    "mongodb_user_id": "c-0001" | "hr-0001",
 *    "user_role": "candidate" | "recruiter",
 *    "sub": "mongodb-user-id",
 *    "iss": "supabase",
 *    "iat": <now>,
 *    "exp": <now + 1h>
 *  }
 */
@Service
@Slf4j
public class SupabaseTokenService {

    private static final long TOKEN_TTL_SECONDS = 3600; // 1 hour

    @Autowired
    private SupabaseConfig supabaseConfig;

    /**
     * Generates a Supabase Realtime JWT for the given user.
     * The JWT is signed with the SUPABASE_JWT_SECRET so Supabase can verify it.
     *
     * @param userId   MongoDB user ID (e.g. "c-0001" or "hr-0001")
     * @param userRole "recruiter" or "candidate"
     * @return Signed JWT string, or null if jwt-secret is not configured
     */
    public String generateRealtimeToken(String userId, String userRole) {
        String jwtSecret = supabaseConfig.getSupabaseJwtSecret();
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            log.warn("SUPABASE_JWT_SECRET is not configured. Cannot issue Supabase Realtime token.");
            return null;
        }

        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

            Instant now = Instant.now();
            Instant expiry = now.plusSeconds(TOKEN_TTL_SECONDS);

            Map<String, Object> claims = new HashMap<>();
            claims.put("role", "authenticated");          // Supabase requires this for RLS
            claims.put("mongodb_user_id", userId);        // Used by RLS policy
            claims.put("user_role", userRole);            // "recruiter" | "candidate"
            claims.put("iss", "supabase");

            String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(userId)
                    .setIssuedAt(Date.from(now))
                    .setExpiration(Date.from(expiry))
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

            log.debug("Supabase Realtime token issued for user={}, role={}, exp={}",
                    userId, userRole, expiry);
            return token;
        } catch (Exception e) {
            log.error("Failed to generate Supabase Realtime token for user={}: {}", userId, e.getMessage());
            return null;
        }
    }

    /**
     * Generates a Supabase Realtime JWT from a UserDTO.
     *
     * @param user UserDTO from JwtRequestFilter
     * @return Signed JWT string, or null on failure
     */
    public String generateRealtimeToken(UserDTO user) {
        if (user == null) return null;
        return generateRealtimeToken(user.getId(), user.getRole());
    }

    /**
     * Returns the token TTL in seconds (for the client to schedule refresh).
     */
    public long getTokenTtlSeconds() {
        return TOKEN_TTL_SECONDS;
    }
}
