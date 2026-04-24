

package com.aibackend.AiBasedEndtoEndSystem.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.Candidate;
import com.aibackend.AiBasedEndtoEndSystem.entity.Recruiter;
import com.aibackend.AiBasedEndtoEndSystem.entity.User;
import com.aibackend.AiBasedEndtoEndSystem.service.MyUserDetailsService;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {
    private static final String AUTH_USER_CACHE_PREFIX = "auth:userDTO:";
    private static final Duration AUTH_USER_CACHE_TTL = Duration.ofDays(1);

    private final JwtUtil jwtUtil;
    private final MyUserDetailsService userService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public JwtRequestFilter(JwtUtil jwtUtil,
                            MyUserDetailsService userService,
                            StringRedisTemplate stringRedisTemplate,
                            ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);

            try {
                Claims claims = jwtUtil.extractAllClaims(jwt);

                String userId = claims.getSubject();
                String role = claims.get("r", String.class);

                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDTO userDTO = getUserContextFromCacheOrDb(userId, role);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDTO,
                                    null,
                                    List.of(new SimpleGrantedAuthority(role))
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

            } catch (ExpiredJwtException e) {
                log.warn("JWT expired for request {}: {}", request.getRequestURI(), e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Token expired. Please login again.\"}");
                return;
            } catch (Exception e) {
                log.warn("JWT authentication failed for request {}: {}", request.getRequestURI(), e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"message\":\"Invalid token.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private UserDTO getUserContextFromCacheOrDb(String userId, String role) {
        String normalizedRole = normalizeRole(role);
        String redisKey = AUTH_USER_CACHE_PREFIX + normalizedRole + ":" + userId;

        String cachedUserJson = null;
        try {
            cachedUserJson = stringRedisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            log.warn("Redis unavailable while reading auth user context for key {}: {}", redisKey, e.getMessage());
        }

        if (StringUtils.hasText(cachedUserJson)) {
            try {
                return objectMapper.readValue(cachedUserJson, UserDTO.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse cached auth user context for key {}. Falling back to DB.", redisKey);
                try {
                    stringRedisTemplate.delete(redisKey);
                } catch (Exception redisDeleteException) {
                    log.warn("Redis unavailable while deleting bad cache key {}: {}", redisKey, redisDeleteException.getMessage());
                }
            }
        }

        Object userEntity = switch (normalizedRole) {
            case "user" -> userService.loadUserEntityById(userId);
            case "candidate" -> userService.loadCandidateById(userId);
            case "recruiter" -> userService.loadRecruiterById(userId);
            default -> throw new RuntimeException("Invalid role in JWT");
        };

        UserDTO userDTO = new UserDTO();
        userDTO.setId(userId);
        userDTO.setRole(role);

        if (userEntity instanceof User u) {
            userDTO.setUsername(u.getName());
            userDTO.setUserEmail(u.getEmail());
        } else if (userEntity instanceof Candidate c) {
            userDTO.setUsername(c.getName());
            userDTO.setUserEmail(c.getEmail());
        } else if (userEntity instanceof Recruiter r) {
            userDTO.setUsername(r.getName());
            userDTO.setUserEmail(r.getEmail());
        }

        try {
            stringRedisTemplate.opsForValue().set(
                    redisKey,
                    objectMapper.writeValueAsString(userDTO),
                    AUTH_USER_CACHE_TTL
            );
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize auth user context for key {}.", redisKey);
        } catch (Exception e) {
            log.warn("Redis unavailable while caching auth user context for key {}: {}", redisKey, e.getMessage());
        }

        return userDTO;
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            throw new RuntimeException("Role is missing in JWT");
        }
        return role.toLowerCase(Locale.ROOT);
    }
}
