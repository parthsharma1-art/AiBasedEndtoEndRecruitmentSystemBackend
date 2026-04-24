package com.aibackend.AiBasedEndtoEndSystem.config;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RedisConfig {
    private final RestTemplate restTemplate;

    public RedisConfig(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${UPSTASH_REDIS_REST_URL}")
    private String redisUrl;

    @Value("${UPSTASH_REDIS_REST_TOKEN}")
    private String token;

    public void set(String key, String value) {
        validateRedisConfig();
        String url = redisUrl + "/set/" + key + "/" + value;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

    public String get(String key) {
        validateRedisConfig();
        String url = redisUrl + "/get/" + key;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
        return response.getBody().get("result").toString();
    }

    private void validateRedisConfig() {
        if (redisUrl == null || redisUrl.isBlank() || token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "Upstash Redis credentials are missing. Set UPSTASH_REDIS_REST_URL and UPSTASH_REDIS_REST_TOKEN.");
        }
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig().serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
