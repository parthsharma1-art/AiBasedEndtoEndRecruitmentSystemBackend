package com.aibackend.AiBasedEndtoEndSystem.controller;

import com.aibackend.AiBasedEndtoEndSystem.entity.User;
import com.aibackend.AiBasedEndtoEndSystem.service.CacheCommonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private CacheCommonService cacheCommonService;

    public TestController(CacheCommonService cacheCommonService) {
        this.cacheCommonService = cacheCommonService;
    }

    @GetMapping("/{id}")
    public User test(@PathVariable String id) {
        return cacheCommonService.getUserById(id);
    }
}

