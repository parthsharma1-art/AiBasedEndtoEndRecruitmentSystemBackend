package com.aibackend.AiBasedEndtoEndSystem.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobPostings;
import com.aibackend.AiBasedEndtoEndSystem.entity.ShortlistEvaluationResult;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications.JobStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CronJobService {

    private final JobPostingService jobPostingService;
    private final JobApplicationService jobApplicationService;
    private final AiResumeEvaluatingService aiResumeEvaluatingService;

    @Scheduled(cron = "${cron.job.time}")
    public void sendScheduledMessages() {
        log.info("Started calculating sum value:{}", Instant.now());
        int sum = 0;
        for (int i = 0; i <= 5; i++) {
            log.info("The value of i is :{}", i);
            sum += i;
        }
        log.info("Value of sum is :{}", sum);
        log.info("Completed calculating sum value");
    }

    @Scheduled(cron = "${cron.job.shortlistEvaluation.time}")
    public List<ShortlistEvaluationResult> evaluateShortlistForAllJobApplications() {
        log.info("evaluateShortlistForAllJobApplications started at {}", Instant.now());
        List<JobPostings> activeJobs = jobPostingService.getAllActiveJobPostings();
        List<ShortlistEvaluationResult> results = new ArrayList<>();
        if (activeJobs == null || activeJobs.isEmpty()) {
            log.info("No active job postings to evaluate");
            return results;
        }
        for (JobPostings job : activeJobs) {
            log.info("Job posting id :{}", job.getId());
            List<JobApplications> applications = jobApplicationService.getAllJobApplicationsDetails(job);
            if (applications == null || applications.isEmpty()) {
                log.debug("No applications for job {}", job.getId());
                continue;
            }
            for (JobApplications application : applications) {
                log.info("Evaluating application {} for job {}", application.getId(), job.getId());
                if (application.getResumeId() == null || application.getResumeId().isBlank()) {
                    log.warn("Skipping application {}: no resumeId", application.getId());
                    continue;
                }
                if (!application.getStatus().equals(JobStatus.APPLIED)) {
                    log.warn("Skipping application {}: status is {} (only APPLIED is evaluated)", application.getId(), application.getStatus());
                    continue;
                }

                try {
                    ShortlistEvaluationResult evaluation = aiResumeEvaluatingService
                            .sendJobPostingAndResumeToShortlistEvaluate(
                                    job,
                                    application.getResumeId(),
                                    application.getCandidateId(),
                                    application.getId());
                    results.add(evaluation);
                } catch (Exception e) {
                    log.error(
                            "Shortlist evaluate failed for job {} application {}: {}",
                            job.getId(),
                            application.getId(),
                            e.getMessage());
                }
            }
        }
        return results;
    }

}
