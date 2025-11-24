package com.aibackend.AiBasedEndtoEndSystem.security;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.Candidate;
import com.aibackend.AiBasedEndtoEndSystem.entity.Recruiter;
import com.aibackend.AiBasedEndtoEndSystem.entity.User;
import com.aibackend.AiBasedEndtoEndSystem.service.MyUserDetailsService;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Component
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final MyUserDetailsService userService;

    public JwtRequestFilter(JwtUtil jwtUtil, MyUserDetailsService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String jwt = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwt = authHeader.substring(7);
        }

        if (jwt != null) {
            try {
                Claims claims = jwtUtil.extractAllClaims(jwt);
                log.info("ALL CLAIMS: {}", claims);

                if (claims != null) {
                    String userId = claims.getSubject().trim();
                    String role = claims.get("role", String.class);
                    log.info("Id for the user is :{}",userId);
                    log.info("Role :{}",role);
                    Object userEntity = switch (role) {
                        case "User" -> userService.loadUserEntityById(userId);
                        case "Candidate" -> userService.loadCandidateById(userId);
                        case "Recruiter" -> userService.loadRecruiterById(userId);
                        default -> throw new RuntimeException("Invalid role in token");
                    };
                    UserDTO userDTO = new UserDTO();
                    userDTO.setId(userId);
                    userDTO.setRole(role);
                    if (userEntity instanceof User u) {
                        userDTO.setUsername(u.getName());
                        userDTO.setUserEmail(u.getEmail());
                    }
                    if (userEntity instanceof Candidate c) {
                        userDTO.setUsername(c.getName());
                        userDTO.setUserEmail(c.getEmail());
                    }
                    if (userEntity instanceof Recruiter r) {
                        userDTO.setUsername(r.getName());
                        userDTO.setUserEmail(r.getEmail());
                    }

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDTO,
                                    null,
                                    Collections.emptyList()
                            );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                log.error("JWT authentication failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
