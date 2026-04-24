package com.aibackend.AiBasedEndtoEndSystem.service;

import com.aibackend.AiBasedEndtoEndSystem.entity.User;
import com.aibackend.AiBasedEndtoEndSystem.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CacheCommonService {

    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    public CacheCommonService(UserRepository userRepository,
                              CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
    }
    @PostConstruct
    public void clearAllCachesOnStartup() {
        cacheManager.getCacheNames()
                .forEach(name -> {
                    log.info("Clearing cache: {}", name);
                    cacheManager.getCache(name).clear();
                });
    }

    @Cacheable(value = "users_v2", key = "#id")
    public User getUserById(String id) {
        log.info("Fetching user from DB using id: {}", id);
        return userRepository.findById(id).orElse(null);
    }
}
