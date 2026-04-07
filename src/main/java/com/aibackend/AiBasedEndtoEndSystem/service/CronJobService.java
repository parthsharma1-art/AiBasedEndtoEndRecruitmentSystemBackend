package com.aibackend.AiBasedEndtoEndSystem.service;

import java.time.Instant;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications.AIShortlistStatus;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications.JobStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CronJobService {

    private final JobApplicationService jobApplicationService;

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

    @Scheduled(cron = "${cron.job.shortlistEvaluation.time:0 */8 * * * *}")
    public void scheduledShortlistEvaluationNotificationsAndStatusUpdates() {
        log.info("scheduledShortlistEvaluationNotificationsAndStatusUpdates started at {}", Instant.now());
        try {
            List<JobApplications> results = jobApplicationService.getApplicationsForProcessing();
            for (JobApplications jobApplications : results) {
                if (AIShortlistStatus.SHORTLISTED.equals(jobApplications.getAiShortlistStatus())) {
                    jobApplicationService.updateJobApplicationAndSendNotification(jobApplications,
                            JobStatus.TEST_SCHEDULED);
                } else {
                    jobApplicationService.updateJobApplicationAndSendNotification(jobApplications, JobStatus.REJECTED);
                }

            }
            log.info("scheduledShortlistEvaluationNotificationsAndStatusUpdates finished, evaluations={}",
                    results.size());
        } catch (Exception e) {
            log.error("scheduledShortlistEvaluationNotificationsAndStatusUpdates failed", e);
        }
    }

}
