package com.aibackend.AiBasedEndtoEndSystem.util;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS512);

    @Data
    public static class Token {
        private String authKey;
    }

    public Key getKey() {
        return key;
    }


    private Integer adminTokenExpiryInSeconds = 72 * 60 * 60;

    public Token generateClientToken(UserDTO userDTO) {
        try {
            log.info("generate client Token request : {}", userDTO);
            if (ObjectUtils.isEmpty(userDTO.getId())) {
                throw new BadException("Invalid user");
            }

            long now = System.currentTimeMillis();
            long expiryMillis = now + adminTokenExpiryInSeconds * 1000L;

            String jwt = Jwts.builder()
                    .setSubject(userDTO.getId()) // user ID as subject
                    .claim("userName", userDTO.getUsername())
                    .claim("userEmail", userDTO.getUserEmail())
                    .claim("userMobileNumber", userDTO.getMobileNumber())
                    .claim("role", userDTO.getRole())
                    .setIssuedAt(new Date(now))
                    .setExpiration(new Date(expiryMillis))
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
            Token token = new Token();
            token.setAuthKey(jwt);
            return token;

        } catch (Exception e) {
            log.info("FAiled to create token");
            return null;
        }
    }


    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean invalidateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            log.info("Token for user {} invalidated", claims.getSubject());
            return true;
        } catch (Exception e) {
            log.warn("Failed to invalidate token: {}", e.getMessage());
            return false;
        }
    }


    public String extractUserObjectId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject(); // this is the user ID
    }

}
