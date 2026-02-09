package com.aibackend.AiBasedEndtoEndSystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
public class AuthAppConfig {

    @Value("${app.config.url.baseurl}")
    private String baseUrl;

    @Value("${app.config.url.frontEndUrl}")
    private String frontEndUrl;

    @Value("${app.config.url.baseurl}")
    private String redirectUriForRecruiter;
}
