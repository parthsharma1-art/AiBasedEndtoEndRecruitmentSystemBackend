package com.aibackend.AiBasedEndtoEndSystem.service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.aibackend.AiBasedEndtoEndSystem.dto.CandidateResumeAtsEvaluationResponse;
import com.aibackend.AiBasedEndtoEndSystem.entity.Candidate;
import com.aibackend.AiBasedEndtoEndSystem.entity.CandidateResumeAtsEvaluation;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.repository.CandidateResumeAtsEvaluationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CandidateResumeAtsService {

    private final RestTemplate restTemplate;
    private final FileStorageService fileStorageService;
    private final CandidateService candidateService;
    private final CandidateResumeAtsEvaluationRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${shortlist.atsScore.url}")
    private String atsScoreUrl;

    public CandidateResumeAtsEvaluationResponse evaluateAndSave(String candidateId, MultipartFile resume) {
        log.info("Resume ats score api for candidate :{}", candidateId);
        if (resume == null || resume.isEmpty()) {
            throw new BadException("Resume file is required");
        }
        Candidate candidate = candidateService.getCandidateById(candidateId);
        if (candidate == null) {
            throw new BadException("Candidate not found for ID: " + candidateId);
        }
        try {
            byte[] resumeBytes = resume.getBytes();
            String resumeId = fileStorageService.storeFile(resume);
            CandidateResumeAtsEvaluationResponse aiResponse = callAtsScoreApi(resumeBytes,
                    resume.getOriginalFilename());
            log.info("Ai response :{}", aiResponse);
            CandidateResumeAtsEvaluation entity = new CandidateResumeAtsEvaluation();
            entity.setCandidateId(candidateId);
            entity.setResumeId(resumeId);
            entity.setAtsScore(aiResponse.getAtsScore());
            entity.setFeedback(toEntityFeedback(aiResponse.getFeedback()));
            Instant now = Instant.now();
            entity.setEvaluatedAt(now);
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);

            CandidateResumeAtsEvaluation saved = repository.save(entity);
            return toResponse(saved);
        } catch (IOException e) {
            throw new BadException("Failed to read resume file");
        }
    }

    public List<CandidateResumeAtsEvaluationResponse> getEvaluations(String candidateId) {
        Candidate candidate = candidateService.getCandidateById(candidateId);
        if (candidate == null) {
            throw new BadException("Candidate not found for ID: " + candidateId);
        }
        List<CandidateResumeAtsEvaluation> evaluations = repository
                .findByCandidateIdOrderByEvaluatedAtDesc(candidateId);
        return evaluations.stream().map(CandidateResumeAtsService::toResponse).toList();
    }

    private CandidateResumeAtsEvaluationResponse callAtsScoreApi(byte[] resumeBytes, String originalFilename) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("resume", new ByteArrayResource(resumeBytes) {
            @Override
            public String getFilename() {
                return originalFilename != null && !originalFilename.isBlank() ? originalFilename : "resume.pdf";
            }
        });
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(atsScoreUrl, request, String.class);
            if (response.getBody() == null || response.getBody().isBlank()) {
                throw new BadException("ATS score API returned empty response");
            }
            return objectMapper.readValue(response.getBody(), CandidateResumeAtsEvaluationResponse.class);
        } catch (RestClientException e) {
            log.error("ATS score API call failed: {}", e.getMessage());
            throw new BadException("Failed to evaluate resume ATS score");
        } catch (Exception e) {
            log.error("Failed to parse ATS score API response: {}", e.getMessage());
            throw new BadException("Invalid response from ATS score API");
        }
    }

    private static CandidateResumeAtsEvaluation.Feedback toEntityFeedback(
            CandidateResumeAtsEvaluationResponse.Feedback source) {
        if (source == null) {
            return null;
        }
        CandidateResumeAtsEvaluation.Feedback target = new CandidateResumeAtsEvaluation.Feedback();
        target.setLevel(source.getLevel());
        target.setStrengths(source.getStrengths());
        target.setImprovementAreas(source.getImprovementAreas());
        return target;
    }

    private static CandidateResumeAtsEvaluationResponse toResponse(CandidateResumeAtsEvaluation source) {
        CandidateResumeAtsEvaluationResponse response = new CandidateResumeAtsEvaluationResponse();
        response.setId(source.getId());
        response.setCandidateId(source.getCandidateId());
        response.setResumeId(source.getResumeId());
        response.setAtsScore(source.getAtsScore());
        response.setEvaluatedAt(source.getEvaluatedAt());
        if (source.getFeedback() != null) {
            CandidateResumeAtsEvaluationResponse.Feedback feedback = new CandidateResumeAtsEvaluationResponse.Feedback();
            feedback.setLevel(source.getFeedback().getLevel());
            feedback.setStrengths(source.getFeedback().getStrengths());
            feedback.setImprovementAreas(source.getFeedback().getImprovementAreas());
            response.setFeedback(feedback);
        }
        return response;
    }
}
