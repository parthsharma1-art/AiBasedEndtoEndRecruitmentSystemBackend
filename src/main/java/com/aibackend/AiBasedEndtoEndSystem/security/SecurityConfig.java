package com.aibackend.AiBasedEndtoEndSystem.security;

import com.aibackend.AiBasedEndtoEndSystem.service.UserService;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    public SecurityConfig(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtRequestFilter jwtFilter = new JwtRequestFilter();
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/login", "/public/register", "/public/all/**").permitAll()
                        .anyRequest().authenticated()
                )
                // don't use form login for API
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
