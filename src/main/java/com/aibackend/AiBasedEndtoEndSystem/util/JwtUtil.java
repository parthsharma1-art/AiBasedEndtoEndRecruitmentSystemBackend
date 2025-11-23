package com.aibackend.AiBasedEndtoEndSystem.util;


import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private final String SECRET = "THIS_IS_A_LONG_SECRET_KEY_FOR_JWT_512_BITS_EXAMPLE_12345678901234567890";

    public String generateToken(UserDTO user) {

        Map<String, Object> claims = new HashMap<>();

        // ADD USER ID AS STRING (IMPORTANT)
        claims.put("userId", user.getId().toHexString());
        claims.put("role", user.getRole().name());

        return Jwts.builder()
                .subject(user.getUserEmail())
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000)) // 1 day
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())     // NEW WAY for 0.12.x
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }
}
