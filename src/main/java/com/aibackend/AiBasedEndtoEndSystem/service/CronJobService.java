package com.aibackend.AiBasedEndtoEndSystem.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications.JobStatus;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobPostings;
import com.aibackend.AiBasedEndtoEndSystem.entity.ShortlistEvaluationResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CronJobService {

    private final JobPostingService jobPostingService;
    private final JobApplicationService jobApplicationService;
    private final AiResumeEvaluatingService aiResumeEvaluatingService;

    @Scheduled(cron = "${cron.job.time:0 0 */14 * * *}")
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

    // @Scheduled(cron = "${cron.job.shortlistEvaluation.time:0 0 */14 * * *}")
    public List<ShortlistEvaluationResult> evaluateShortlistForAllJobApplications() {
        log.info("evaluateShortlistForAllJobApplications started at {}", Instant.now());
        List<JobPostings> activeJobs = jobPostingService.getAllActiveJobPostings();
        if (activeJobs == null || activeJobs.isEmpty()) {
            log.info("No active job postings to evaluate");
            return new ArrayList<>();
        }
        Map<JobPostings, List<JobApplications>> jobApplicationsMap = new HashMap<>();
        for (JobPostings job : activeJobs) {
            List<JobApplications> applications = jobApplicationService.getAllJobApplicationsDetailsByStatusApplied(job,
                    JobStatus.APPLIED);
            if (applications == null || applications.isEmpty()) {
                log.debug("No applications for job {}", job.getId());
                continue;
            }
            List<JobApplications> eligible = applications.stream()
                    .filter(a -> a != null && a.getResumeId() != null && !a.getResumeId().isBlank())
                    .collect(Collectors.toList());
            if (!eligible.isEmpty()) {
                jobApplicationsMap.put(job, eligible);
            }
        }
        if (jobApplicationsMap.isEmpty()) {
            log.info("No job applications to evaluate");
            return new ArrayList<>();
        }
        log.info("Job applications to evaluate: {}", jobApplicationsMap.values().stream().mapToInt(List::size).sum());
        return aiResumeEvaluatingService.sendBatchShortlistEvaluate(jobApplicationsMap);
    }

}
