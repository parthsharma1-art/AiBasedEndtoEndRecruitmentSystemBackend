package com.aibackend.AiBasedEndtoEndSystem.service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.aibackend.AiBasedEndtoEndSystem.entity.JobPostings;
import com.aibackend.AiBasedEndtoEndSystem.entity.ShortlistEvaluationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiResumeEvaluatingService {

    private final RestTemplate restTemplate;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final ShortlistEvaluationResultService shortlistEvaluationResultService;

    @Value("${shortlist.evaluate.url}")
    private String shortlistEvaluateUrl;

    public ShortlistEvaluationResult sendJobPostingAndResumeToShortlistEvaluate(
            JobPostings jobPosting,
            String resumeId,
            String candidateId,
            String jobApplicationId) {
        if (resumeId == null || resumeId.isBlank()) {
            throw new IllegalArgumentException("resumeId is missing");
        }
        GridFsResource resumeResource = fileStorageService.getFile(resumeId);
        try {
            byte[] resumeBytes = resumeResource.getInputStream().readAllBytes();
            String filename = resumeResource.getFilename() != null ? resumeResource.getFilename() : "resume.pdf";
            return evaluateAndPersist(
                    jobPosting,
                    resumeBytes,
                    filename,
                    candidateId,
                    jobPosting != null ? jobPosting.getId() : null,
                    jobApplicationId,
                    resumeId);
        } catch (IOException e) {
            log.error("Error occurred while creating Shortlist evaluation entity :{}", e.getMessage());
            return null;
        }
    }

    public ShortlistEvaluationResult sendJobPostingAndResumeToShortlistEvaluate(
            JobPostings jobPosting,
            byte[] resumeBytes,
            String resumeFileName) {
        return evaluateAndPersist(
                jobPosting,
                resumeBytes,
                resumeFileName,
                null,
                jobPosting != null ? jobPosting.getId() : null,
                null,
                null);
    }

    public ShortlistEvaluationResult sendJobPostingAndResumeToShortlistEvaluate(
            JobPostings jobPosting,
            byte[] resumeBytes,
            String resumeFileName,
            String candidateId,
            String jobPostingId) {
        return evaluateAndPersist(
                jobPosting,
                resumeBytes,
                resumeFileName,
                candidateId,
                jobPostingId,
                null,
                null);
    }

    private ShortlistEvaluationResult evaluateAndPersist(
            JobPostings jobPosting,
            byte[] resumeBytes,
            String resumeFileName,
            String candidateId,
            String jobPostingId,
            String jobApplicationId,
            String resumeGridFsId) {
        String jobJson = buildJobJson(jobPosting);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("job", jobJson);
        body.add("resume", new ByteArrayResource(resumeBytes) {
            @Override
            public String getFilename() {
                return resumeFileName != null && !resumeFileName.isBlank() ? resumeFileName : "resume.pdf";
            }
        });

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(shortlistEvaluateUrl, request, String.class);
            log.info("Shortlist evaluate API status: {}", response.getStatusCode());
            log.info("Response from ai service :{}", response);
            String responseBody = response.getBody();
            return shortlistEvaluationResultService.persistShortlistEvaluationResult(
                    responseBody,
                    candidateId,
                    jobPostingId,
                    jobApplicationId,
                    resumeGridFsId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Shortlist API returned empty body or JSON that could not be parsed into ShortlistEvaluationResult"));
        } catch (RestClientException e) {
            log.error("Shortlist evaluate API call failed: {}", e.getMessage());
            throw e;
        }
    }

    private String buildJobJson(JobPostings job) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", job.getTitle() != null ? job.getTitle() : "");
        map.put("description", job.getDescription() != null ? job.getDescription() : "");
        map.put("skillsRequired", job.getSkillsRequired() != null ? job.getSkillsRequired() : List.of());
        map.put("experienceRequired", job.getExperienceRequired());
        map.put("profile", job.getProfile() != null ? job.getProfile() : "");
        map.put("jobType", job.getJobType() != null ? job.getJobType().name() : "");
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize job posting to JSON", e);
        }
    }
}
