package com.aibackend.AiBasedEndtoEndSystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

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

    @PostMapping
    public List<ShortlistEvaluationResult> evaluateShortlistForAllJobApplications() {
        log.info("Starting the work for shortlisting");
        List<ShortlistEvaluationResult> shortlistEvaluationResults = new ArrayList<>();
        List<JobPostings> allJobs = jobPostingService.getAllActiveJobPostings();
        if (ObjectUtils.isEmpty(allJobs)) {
            return shortlistEvaluationResults;
        }
        for (JobPostings job : allJobs) {
            List<JobApplications> applications = jobApplicationService.getAllJobApplicationsDetails(job);
            if (ObjectUtils.isEmpty(applications)) {
                continue;
            }
            for (JobApplications application : applications) {
                if (!ObjectUtils.isEmpty(application.getResumeId()) && !application.getResumeId().isBlank()) {
                    log.info("Resume Id is :{}", application.getResumeId());
                    ShortlistEvaluationResult shortlistEvaluationResult = aiResumeEvaluatingService
                            .sendJobPostingAndResumeToShortlistEvaluate(
                                    job,
                                    application.getResumeId(),
                                    application.getCandidateId(),
                                    application.getId())
                            .get();
                    shortlistEvaluationResults.add(shortlistEvaluationResult);
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("Error in sleeping the thread: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        return shortlistEvaluationResults;
    }

}
