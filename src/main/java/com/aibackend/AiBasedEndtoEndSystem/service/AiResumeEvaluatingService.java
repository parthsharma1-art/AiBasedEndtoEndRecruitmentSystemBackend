package com.aibackend.AiBasedEndtoEndSystem.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.aibackend.AiBasedEndtoEndSystem.entity.JobPostings;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications;
import com.aibackend.AiBasedEndtoEndSystem.entity.ShortlistEvaluationResult;
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

    @Value("${shortlist.evaluate.batchUrl}")
    private String shortlistEvaluateBatchUrl;

    @Value("${shortlist.evaluate.url}")
    private String shortlistEvaluateUrl;

    private static final int BATCH_SIZE = 10;

    public Optional<ShortlistEvaluationResult> sendJobPostingAndResumeToShortlistEvaluate(
            JobPostings jobPosting,
            String resumeId,
            String candidateId,
            String jobApplicationId) {
        log.info("Sending job posting and resume to shortlist evaluate for jobApplicationId={}", jobApplicationId);
        if (resumeId == null || resumeId.isBlank()) {
            log.warn("Skipping shortlist evaluate: resumeId missing for jobApplicationId={}", jobApplicationId);
            return Optional.empty();
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
            log.error("Error reading resume for shortlist evaluation: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ShortlistEvaluationResult> evaluateAndPersist(
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            log.info("Calling shortlist evaluate API for jobApplicationId={}", jobApplicationId);
            ResponseEntity<String> response = restTemplate.postForEntity(shortlistEvaluateUrl, request, String.class);
            log.info("Shortlist evaluate API status: {}", response.getStatusCode());
            String responseBody = response.getBody();
            return shortlistEvaluationResultService.persistShortlistEvaluationResult(
                    responseBody,
                    candidateId,
                    jobPostingId,
                    jobApplicationId,
                    resumeGridFsId);
        } catch (RestClientException e) {
            log.error("Shortlist evaluate API call failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public List<ShortlistEvaluationResult> sendBatchShortlistEvaluate(
            Map<JobPostings, List<JobApplications>> jobApplicationsMap) {
        List<ShortlistEvaluationResult> finalResults = new ArrayList<>();
        if (jobApplicationsMap == null || jobApplicationsMap.isEmpty()) {
            return finalResults;
        }
        for (Map.Entry<JobPostings, List<JobApplications>> entry : jobApplicationsMap.entrySet()) {
            JobPostings job = entry.getKey();
            List<JobApplications> applications = entry.getValue();
            if (job == null || applications == null || applications.isEmpty()) {
                continue;
            }
            List<List<JobApplications>> chunks = partition(applications, BATCH_SIZE);
            for (List<JobApplications> chunk : chunks) {
                try {
                    List<ShortlistEvaluationResult> results = callBatchApi(job, chunk);
                    finalResults.addAll(results);
                } catch (Exception e) {
                    log.error("Batch API failed for job {} chunk size {}",
                            job.getId(), chunk.size(), e);
                }
            }
        }
        return finalResults;
    }

    private List<ShortlistEvaluationResult> callBatchApi(
            JobPostings job,
            List<JobApplications> applications) throws IOException {
        String jobJson = buildJobJson(job);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("job", jobJson);
        List<String> candidateIds = new ArrayList<>();
        List<String> jobIds = new ArrayList<>();
        List<String> appIds = new ArrayList<>();
        List<String> resumeIds = new ArrayList<>();
        for (JobApplications app : applications) {
            if (app == null || app.getResumeId() == null || app.getResumeId().isBlank()) {
                continue;
            }
            GridFsResource file = fileStorageService.getFile(app.getResumeId());
            byte[] bytes = file.getInputStream().readAllBytes();
            ByteArrayResource resource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return file.getFilename() != null ? file.getFilename() : "resume.pdf";
                }
            };
            body.add("resumes", resource);
            candidateIds.add(app.getCandidateId());
            jobIds.add(job.getId());
            appIds.add(app.getId());
            resumeIds.add(app.getResumeId());
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        log.info("Calling batch API with {} resumes", applications.size());
        ResponseEntity<String> response = restTemplate.postForEntity(shortlistEvaluateBatchUrl, request, String.class);
        String responseBody = response.getBody();
        if (responseBody == null || responseBody.isBlank()) {
            throw new RuntimeException("Empty response from batch API");
        }
        Map<String, Object> parsed = objectMapper.readValue(responseBody, Map.class);
        List<Map<String, Object>> results = (List<Map<String, Object>>) parsed.get("results");
        List<ShortlistEvaluationResult> finalResults = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> item = results.get(i);
            String resultJson = objectMapper.writeValueAsString(item.get("result"));
            ShortlistEvaluationResult saved = shortlistEvaluationResultService.persistShortlistEvaluationResult(
                    resultJson,
                    candidateIds.get(i),
                    jobIds.get(i),
                    appIds.get(i),
                    resumeIds.get(i)).orElse(null);
            if (saved != null)
                finalResults.add(saved);
        }
        return finalResults;
    }

    private List<List<JobApplications>> partition(List<JobApplications> list, int size) {
        List<List<JobApplications>> parts = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            parts.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return parts;
    }

    private String buildJobJson(JobPostings job) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("title", job.getTitle());
            map.put("description", job.getDescription());
            map.put("skillsRequired", job.getSkillsRequired());
            map.put("experienceRequired", job.getExperienceRequired());
            map.put("profile", job.getProfile());
            map.put("jobType", job.getJobType() != null ? job.getJobType().name() : "");

            return objectMapper.writeValueAsString(map);

        } catch (Exception e) {
            throw new RuntimeException("Error building job JSON", e);
        }
    }
}