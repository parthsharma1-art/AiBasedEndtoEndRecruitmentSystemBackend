package com.aibackend.AiBasedEndtoEndSystem.util;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.User;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET =
            "MyVeryLongSecretKeyForJWTGenerationThatIsAtLeast64CharactersLongForHS512Algorithm";
    private static final long EXPIRATION = 24 * 60 * 60 * 1000; // 1 day

    private final SecretKey secretKey;

    public JwtUtil() {
        this.secretKey = Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    public String generateToken(UserDTO user) {
        User.Role role = (user.getRole() != null) ? user.getRole() : User.Role.USER;
        return Jwts.builder()
                .setSubject(user.getUserEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(secretKey)
                .claim("userId", user.getId())
                .claim("role", role)                  // IMPORTANT: Store role in JWT
                .compact();
    }

    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw ex;
        } catch (JwtException ex) {
            throw ex;
        }
    }
}
