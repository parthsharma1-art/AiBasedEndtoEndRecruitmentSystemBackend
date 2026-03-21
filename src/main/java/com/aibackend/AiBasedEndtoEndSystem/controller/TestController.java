package com.aibackend.AiBasedEndtoEndSystem.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aibackend.AiBasedEndtoEndSystem.entity.ShortlistEvaluationResult;
import com.aibackend.AiBasedEndtoEndSystem.service.CronJobService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/test/shortlist-evaluate")
@Slf4j
public class TestController {
    @Autowired
    private CronJobService cronJobService;

    @PostMapping
    public List<ShortlistEvaluationResult> evaluateShortlistForAllJobApplications() {
        log.info("Starting the cron job work for shortlisting");
        return cronJobService.evaluateShortlistForAllJobApplications();
    }

}
