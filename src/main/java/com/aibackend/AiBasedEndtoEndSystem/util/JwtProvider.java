package com.aibackend.AiBasedEndtoEndSystem.util;

import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtProvider {

    private final Key key;

    public JwtProvider() {
        String secret = "my-super-secret-key-that-is-very-long-for-hs256";
        key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Key getKey() {
        return key;
    }
}


