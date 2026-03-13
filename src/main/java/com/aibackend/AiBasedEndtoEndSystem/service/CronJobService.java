package com.aibackend.AiBasedEndtoEndSystem.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;


@Service
@Slf4j
public class CronJobService {

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
}