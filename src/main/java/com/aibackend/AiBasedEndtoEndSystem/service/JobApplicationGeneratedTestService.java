package com.aibackend.AiBasedEndtoEndSystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplicationGeneratedTest;
import com.aibackend.AiBasedEndtoEndSystem.repository.JobApplicationGeneratedTestRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JobApplicationGeneratedTestService {
    @Autowired
    private JobApplicationGeneratedTestRepository repository;

    public JobApplicationGeneratedTest getJobApplicationGeneratedTestByJobPostingId(String jobPostingId) {
        log.info("Get job Application generated test for job posting Id {}", jobPostingId);
        JobApplicationGeneratedTest jobApplicationGeneratedTest = repository.findByJobApplicationId(jobPostingId)
                .orElse(null);
        return jobApplicationGeneratedTest;
    }

}
