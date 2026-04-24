package com.aibackend.AiBasedEndtoEndSystem.security;

import com.aibackend.AiBasedEndtoEndSystem.service.MyUserDetailsService;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.cors.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final MyUserDetailsService userDetailsService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtUtil jwtUtil,
                          MyUserDetailsService userDetailsService,
                          StringRedisTemplate stringRedisTemplate,
                          ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//
//        JwtRequestFilter jwtFilter = new JwtRequestFilter(jwtUtil, userDetailsService);
//
//        http
//                .cors(cors -> {})   // 🔥 IMPORTANT (ENABLE CORS)
//                .csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers("/public/**","/recruiter/**","/candidate/**", "api/file/**").permitAll()
//
//                        .anyRequest().authenticated()
//                )
//                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
//
//        return http.build();
//    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        JwtRequestFilter jwtFilter =
                new JwtRequestFilter(jwtUtil, userDetailsService, stringRedisTemplate, objectMapper);

        http
                .cors(cors -> {
                })
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/public/**",
                                "/recruiter/**",
                                "/candidate/**",
                                "/file/**"  ,
                                "/api/auth/**",
                                "/auth/**",
                                "/test/**",
                                "/api/checkout/**",
                                "/api/webhook/**",
                                "/webhook/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(false); // ✅ MUST be false with "*"

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

}
