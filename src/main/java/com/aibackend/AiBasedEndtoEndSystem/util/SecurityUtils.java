package com.aibackend.AiBasedEndtoEndSystem.util;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Key;

@Slf4j
public class SecurityUtils {

    /**
     * Returns the currently logged-in user from the SecurityContext (set by JwtRequestFilter).
     * Use this in controllers instead of reading the token from headers.
     * Requires a valid JWT in the Authorization header — the filter populates the context.
     */
    public static UserDTO getLoggedInUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null || !(auth.getPrincipal() instanceof UserDTO)) {
            return null;
        }
        return (UserDTO) auth.getPrincipal();
    }

    public static UserDTO getLoggedInUser(String token, Key key) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
            UserDTO userDTO = new UserDTO();
            userDTO.setId(claims.getSubject());                           // "sub" claim
            userDTO.setUsername(claims.get("userName", String.class));    // custom claim
            userDTO.setUserEmail(claims.get("userEmail", String.class));  // custom claim
            userDTO.setMobileNumber(claims.get("userMobileNumber", String.class));
            return userDTO;

        } catch (Exception e) {
            log.error("JWT parsing failed: {}", e.getMessage());
            return null;
        }
    }


}
