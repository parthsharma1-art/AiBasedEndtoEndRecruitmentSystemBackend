package com.aibackend.AiBasedEndtoEndSystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobPostings;
import com.aibackend.AiBasedEndtoEndSystem.entity.ShortlistEvaluationResult;
import com.aibackend.AiBasedEndtoEndSystem.service.AiResumeEvaluatingService;
import com.aibackend.AiBasedEndtoEndSystem.service.JobApplicationService;
import com.aibackend.AiBasedEndtoEndSystem.service.JobPostingService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/test/shortlist-evaluate")
@Slf4j
public class TestController {
    @Autowired
    private AiResumeEvaluatingService aiResumeEvaluatingService;
    @Autowired
    private JobApplicationService jobApplicationService;
    @Autowired
    private JobPostingService jobPostingService;

    @PostMapping("/{jobApplicationId}")
    public ShortlistEvaluationResult evaluateShortlistForAllJobApplications(@PathVariable String jobApplicationId) {
        log.info("Starting the work for shortlisting");
        log.info(" JOb application resume Id :{}", jobApplicationId);
        JobApplications application = jobApplicationService.getJobApplicationById(jobApplicationId);
        JobPostings jobPostings = jobPostingService.getJobPostingById(application.getJobId());

        ShortlistEvaluationResult shortlistEvaluationResult = null;
        if (!ObjectUtils.isEmpty(application.getResumeId()) && !application.getResumeId().isBlank()) {
            log.info("Resume Id is :{}", application.getResumeId());
            shortlistEvaluationResult = aiResumeEvaluatingService.sendJobPostingAndResumeToShortlistEvaluate(
                    jobPostings,
                    application.getResumeId(),
                    application.getCandidateId(),
                    application.getId()).get();
        }
        return shortlistEvaluationResult;
    }

}
