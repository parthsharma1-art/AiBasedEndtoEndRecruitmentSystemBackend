package com.aibackend.AiBasedEndtoEndSystem.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications;
import com.aibackend.AiBasedEndtoEndSystem.entity.ShortlistEvaluationResult;
import com.aibackend.AiBasedEndtoEndSystem.repository.JobApplicationRepository;
import com.aibackend.AiBasedEndtoEndSystem.repository.ShortlistEvaluationResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShortlistEvaluationResultService {

    private final ShortlistEvaluationResultRepository repository;
    private final ObjectMapper objectMapper;
    private final JobApplicationRepository jobApplicationRepository;

    public ShortlistEvaluationResult getShortlistEvaluationForJobApplication(String jobApplicationID) {
        log.info("Get Shortlist Evaluation for Job Application :{}", jobApplicationID);
        return repository.findFirstByJobApplicationIdOrderByEvaluatedAtDesc(jobApplicationID).orElse(null);
    }

    public Optional<ShortlistEvaluationResult> persistShortlistEvaluationResult(
            String responseBody,
            String candidateId,
            String jobPostingId,
            String jobApplicationId,
            String resumeGridFsId) {
        if (responseBody == null || responseBody.isBlank()) {
            return Optional.empty();
        }
        try {
            ShortlistEvaluationResult stored = objectMapper.readValue(responseBody, ShortlistEvaluationResult.class);
            stored.setId(null);
            stored.setCandidateId(candidateId);
            stored.setJobPostingId(jobPostingId);
            stored.setJobApplicationId(jobApplicationId);
            stored.setResumeId(resumeGridFsId);
            Instant now = Instant.now();
            stored.setEvaluatedAt(now);
            stored.setCreatedAt(now);
            stored.setUpdatedAt(now);
            ShortlistEvaluationResult saved = repository.save(stored);
            log.info("Saved shortlist evaluation result id={}", saved.getId());
            updateJobApplicationStatusIfShortlisted(saved.getShortlisted(), jobApplicationId);
            return Optional.of(saved);
        } catch (Exception e) {
            log.warn("Could not parse or save shortlist evaluation response: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void updateJobApplicationStatusIfShortlisted(Boolean shortlisted, String jobApplicationId) {
        if (!Boolean.TRUE.equals(shortlisted) || jobApplicationId == null || jobApplicationId.isBlank()) {
            return;
        }
        JobApplications jobApplications =
                jobApplicationRepository.findById(jobApplicationId).orElse(null);
        if (jobApplications == null) {
            log.warn("Job application {} not found; cannot set SHORTLISTED", jobApplicationId);
            return;
        }
        jobApplications.setStatus(JobApplications.JobStatus.SHORTLISTED);
        jobApplications.setUpdatedAt(Instant.now());
        jobApplicationRepository.save(jobApplications);
        log.info("Job application {} status set to SHORTLISTED (AI shortlist)", jobApplicationId);
    }
}
