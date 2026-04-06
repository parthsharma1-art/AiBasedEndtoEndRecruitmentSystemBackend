package com.aibackend.AiBasedEndtoEndSystem.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.aibackend.AiBasedEndtoEndSystem.entity.ShortlistEvaluationResult;
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
    private final JobApplicationService jobApplicationService;

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
            jobApplicationService.updateJobApplicationStatusIfShortlisted(saved.getShortlisted(), jobApplicationId);
            return Optional.of(saved);
        } catch (Exception e) {
            log.warn("Could not parse or save shortlist evaluation response: {}", e.getMessage());
            return Optional.empty();
        }
    }

}
