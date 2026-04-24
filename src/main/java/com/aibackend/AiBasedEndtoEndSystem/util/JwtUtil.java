

package com.aibackend.AiBasedEndtoEndSystem.util;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.Locale;

@Getter
@Slf4j
@Component
public class JwtUtil {
    private static final String AUTH_USER_CACHE_PREFIX = "auth:userDTO:";
    private static final Duration AUTH_USER_CACHE_TTL = Duration.ofDays(1);

    private static final String SECRET =
            "AIBasedEndToEndRecruitmentSystemSecretKey1234567890";

    private final Key key =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private static final long TOKEN_EXPIRY_SECONDS = 72 * 60 * 60; // 72 hours
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public JwtUtil(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Data
    public static class Token {
        private String authKey;
    }

    public Token generateClientToken(UserDTO userDTO) {

        if (ObjectUtils.isEmpty(userDTO)
                || ObjectUtils.isEmpty(userDTO.getId())
                || ObjectUtils.isEmpty(userDTO.getRole())) {
            throw new BadException("Invalid user details for token generation");
        }

        long now = System.currentTimeMillis();
        long expiryMillis = now + TOKEN_EXPIRY_SECONDS * 1000;

        String jwt = Jwts.builder()
                .setSubject(userDTO.getId())
                .claim("r", userDTO.getRole().toLowerCase())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(expiryMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        Token token = new Token();
        token.setAuthKey(jwt);
        cacheUserContext(userDTO);
        return token;
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
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("r", String.class);
    }

    private void cacheUserContext(UserDTO userDTO) {
        log.info("Storing User DTO in cache :{}");
        String normalizedRole = userDTO.getRole().toLowerCase(Locale.ROOT);
        String redisKey = AUTH_USER_CACHE_PREFIX + normalizedRole + ":" + userDTO.getId();
        try {
            stringRedisTemplate.opsForValue().set(
                    redisKey,
                    objectMapper.writeValueAsString(userDTO),
                    AUTH_USER_CACHE_TTL
            );
        } catch (JsonProcessingException e) {
            log.warn("Failed to cache auth user context for key {}.", redisKey);
        }
    }
}
