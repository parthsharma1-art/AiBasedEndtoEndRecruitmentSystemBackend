package com.aibackend.AiBasedEndtoEndSystem.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimiterService {

    @Value("${spring.data.redis.url}")
    private String redisUrl;

    private ProxyManager<byte[]> proxyManager;

    @PostConstruct
    public void init() {
        RedisClient redisClient = RedisClient.create(redisUrl);
        this.proxyManager = LettuceBasedProxyManager.builderFor(redisClient).build();
    }

    public Bucket resolveBucket(String key) {
        Bandwidth limit = Bandwidth.classic(50, Refill.greedy(50, Duration.ofMinutes(1)));
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(limit)
                .build();

        // Retrieve bucket from Redis, or create a new one if it doesn't exist yet
        return proxyManager.builder().build(key.getBytes(), configuration);
    }
}
