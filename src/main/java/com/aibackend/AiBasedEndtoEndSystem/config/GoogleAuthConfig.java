package com.aibackend.AiBasedEndtoEndSystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
public class GoogleAuthConfig {

    @Value("${app.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${app.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${app.oauth2.client.registration.google.redirectUriForRecruiter}")
    private String redirectUriForRecruiter;

    @Value("${app.oauth2.client.registration.google.redirectUriForCandidate}")
    private String redirectUriForCandidate;

}