package com.aibackend.AiBasedEndtoEndSystem.security;

import com.aibackend.AiBasedEndtoEndSystem.service.CandidateService;
import com.aibackend.AiBasedEndtoEndSystem.service.RecruiterService;
import com.aibackend.AiBasedEndtoEndSystem.service.UserService;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private RecruiterService hrService;
    @Autowired
    private CandidateService candidateService;
    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String jwt = authHeader.substring(7);
        Claims claims;
        try {
            claims = jwtUtil.validateAndGetClaims(jwt);
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }
        String username = claims.getSubject();
        ObjectId userIdStr = claims.get("userId", ObjectId.class);    // or Integer/Long if you stored as number
        String userType = claims.get("userType", String.class).toLowerCase();   // "hr" | "candidate" | "user"

        if (userType == null || userIdStr == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Object principal = null;
        try {
            switch (userType) {
                case "hr":
                    principal = hrService.findById(userIdStr);        // returns Hr
                    break;
                case "candidate":
                    principal = candidateService.findById(userIdStr); // returns Candidate
                    break;
                default:
                    principal = userService.findById(userIdStr); // returns User
            }
        } catch (NumberFormatException nfe) {
            filterChain.doFilter(request, response);
            return;
        }
        if (principal != null) {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
