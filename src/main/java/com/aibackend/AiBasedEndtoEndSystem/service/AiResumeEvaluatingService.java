package com.aibackend.AiBasedEndtoEndSystem.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// @Service
// @Slf4j
// @RequiredArgsConstructor
// public class AiResumeEvaluatingService {

//     private final RestTemplate restTemplate;
//     private final FileStorageService fileStorageService;
//     private final ObjectMapper objectMapper;
//     private final ShortlistEvaluationResultService shortlistEvaluationResultService;

//     @Value("${shortlist.evaluate.url}")
//     private String shortlistEvaluateUrl;

//     @Value("${shortlist.evaluate.batchUrl:}")
//     private String shortlistEvaluateBatchUrl;

//     public ShortlistEvaluationResult sendJobPostingAndResumeToShortlistEvaluate(
//             JobPostings jobPosting,
//             String resumeId,
//             String candidateId,
//             String jobApplicationId) {
//         if (resumeId == null || resumeId.isBlank()) {
//             throw new IllegalArgumentException("resumeId is missing");
//         }
//         GridFsResource resumeResource = fileStorageService.getFile(resumeId);
//         try {
//             byte[] resumeBytes = resumeResource.getInputStream().readAllBytes();
//             String filename = resumeResource.getFilename() != null ? resumeResource.getFilename() : "resume.pdf";
//             return evaluateAndPersist(
//                     jobPosting,
//                     resumeBytes,
//                     filename,
//                     candidateId,
//                     jobPosting != null ? jobPosting.getId() : null,
//                     jobApplicationId,
//                     resumeId);
//         } catch (IOException e) {
//             log.error("Error occurred while creating Shortlist evaluation entity :{}", e.getMessage());
//             return null;
//         }
//     }

//     public List<ShortlistEvaluationResult> sendBatchShortlistEvaluate(
//             Map<JobPostings, List<JobApplications>> jobApplicationsMap) {
//         List<ShortlistEvaluationResult> results = new ArrayList<>();
//         if (jobApplicationsMap == null || jobApplicationsMap.isEmpty()) {
//             return results;
//         }

//         if (shortlistEvaluateBatchUrl != null && !shortlistEvaluateBatchUrl.isBlank()) {
//             try {
//                 BatchEvaluateRequest payload = buildBatchRequest(jobApplicationsMap);
//                 log.info("Batch shortlist evaluate request: {}", payload);
//                 if (!payload.items.isEmpty()) {
//                     HttpHeaders headers = new HttpHeaders();
//                     headers.setContentType(MediaType.APPLICATION_JSON);
//                     HttpEntity<BatchEvaluateRequest> request = new HttpEntity<>(payload, headers);

//                     ResponseEntity<String> response = restTemplate.postForEntity(shortlistEvaluateBatchUrl, request,
//                             String.class);
//                     String responseBody = response.getBody();
//                     log.info("Batch shortlist evaluate response: {}", responseBody);
//                     if (responseBody == null || responseBody.isBlank()) {
//                         throw new IllegalStateException("Batch shortlist API returned empty response");
//                     }

//                     List<ShortlistEvaluationResult> evaluations = objectMapper.readValue(
//                             responseBody, new TypeReference<List<ShortlistEvaluationResult>>() {
//                             });
//                     log.info("Batch shortlist evaluate results: {}", evaluations);
//                     // Persist with ids from the original request items (assumes response order
//                     // matches request order).
//                     shortlistEvaluationResultService.persistShortlistEvaluationResults(
//                             evaluations,
//                             payload.items.stream().map(i -> i.candidateId).toList(),
//                             payload.items.stream().map(i -> i.jobPostingId).toList(),
//                             payload.items.stream().map(i -> i.jobApplicationId).toList(),
//                             payload.items.stream().map(i -> i.resumeId).toList());

//                     return evaluations;
//                 }
//             } catch (Exception e) {
//                 log.warn("Batch shortlist API failed, falling back to per-application calls: {}", e.getMessage());
//             }
//         }

//         // Fallback: per-application calls (existing behavior).
//         for (Map.Entry<JobPostings, List<JobApplications>> entry : jobApplicationsMap.entrySet()) {
//             JobPostings job = entry.getKey();
//             List<JobApplications> applications = entry.getValue();
//             if (job == null || applications == null || applications.isEmpty()) {
//                 continue;
//             }
//             applications.parallelStream().forEach(application -> {
//                 try {
//                     ShortlistEvaluationResult evaluation =
//                             sendJobPostingAndResumeToShortlistEvaluate(
//                                     job,
//                                     application.getResumeId(),
//                                     application.getCandidateId(),
//                                     application.getId());
//                     synchronized (results) {
//                         if (evaluation != null) results.add(evaluation);
//                     }
//                 } catch (Exception e) {
//                     log.error("Fallback failed for {}", application.getId());
//                 }
//             });
//         }
//         return results;
//     }

//     private ShortlistEvaluationResult evaluateAndPersist(
//             JobPostings jobPosting,
//             byte[] resumeBytes,
//             String resumeFileName,
//             String candidateId,
//             String jobPostingId,
//             String jobApplicationId,
//             String resumeGridFsId) {
//         String jobJson = buildJobJson(jobPosting);
//         MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//         body.add("job", jobJson);
//         body.add("resume", new ByteArrayResource(resumeBytes) {
//             @Override
//             public String getFilename() {
//                 return resumeFileName != null && !resumeFileName.isBlank() ? resumeFileName : "resume.pdf";
//             }
//         });

//         HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body);
//         try {
//             ResponseEntity<String> response = restTemplate.postForEntity(shortlistEvaluateUrl, request, String.class);
//             log.info("Shortlist evaluate API status: {}", response.getStatusCode());
//             log.info("Response from ai service :{}", response);
//             String responseBody = response.getBody();
//             return shortlistEvaluationResultService.persistShortlistEvaluationResult(
//                     responseBody,
//                     candidateId,
//                     jobPostingId,
//                     jobApplicationId,
//                     resumeGridFsId)
//                     .orElseThrow(() -> new IllegalStateException(
//                             "Shortlist API returned empty body or JSON that could not be parsed into ShortlistEvaluationResult"));
//         } catch (RestClientException e) {
//             log.error("Shortlist evaluate API call failed: {}", e.getMessage());
//             throw e;
//         }
//     }

//     private String buildJobJson(JobPostings job) {
//         Map<String, Object> map = new LinkedHashMap<>();
//         map.put("title", job.getTitle() != null ? job.getTitle() : "");
//         map.put("description", job.getDescription() != null ? job.getDescription() : "");
//         map.put("skillsRequired", job.getSkillsRequired() != null ? job.getSkillsRequired() : List.of());
//         map.put("experienceRequired", job.getExperienceRequired());
//         map.put("profile", job.getProfile() != null ? job.getProfile() : "");
//         map.put("jobType", job.getJobType() != null ? job.getJobType().name() : "");
//         try {
//             return objectMapper.writeValueAsString(map);
//         } catch (JsonProcessingException e) {
//             throw new RuntimeException("Failed to serialize job posting to JSON", e);
//         }
//     }

//     private BatchEvaluateRequest buildBatchRequest(Map<JobPostings, List<JobApplications>> jobApplicationsMap) {
//         BatchEvaluateRequest req = new BatchEvaluateRequest();
//         for (Map.Entry<JobPostings, List<JobApplications>> entry : jobApplicationsMap.entrySet()) {
//             JobPostings job = entry.getKey();
//             List<JobApplications> apps = entry.getValue();
//             if (job == null || apps == null || apps.isEmpty()) {
//                 continue;
//             }
//             String jobJson = buildJobJson(job);
//             for (JobApplications application : apps) {
//                 if (application == null || application.getResumeId() == null || application.getResumeId().isBlank()) {
//                     continue;
//                 }
//                 GridFsResource resumeResource = fileStorageService.getFile(application.getResumeId());
//                 try {
//                     byte[] resumeBytes = resumeResource.getInputStream().readAllBytes();
//                     String fileName = resumeResource.getFilename() != null ? resumeResource.getFilename()
//                             : "resume.pdf";
//                     BatchEvaluateRequest.Item item = new BatchEvaluateRequest.Item();
//                     item.job = jobJson;
//                     item.resumeBase64 = Base64.getEncoder().encodeToString(resumeBytes);
//                     item.resumeFileName = fileName;
//                     item.resumeId = application.getResumeId();
//                     item.candidateId = application.getCandidateId();
//                     item.jobApplicationId = application.getId();
//                     item.jobPostingId = job.getId();
//                     req.items.add(item);
//                 } catch (IOException e) {
//                     log.warn("Skipping resume {} for application {}: {}", application.getResumeId(),
//                             application.getId(), e.getMessage());
//                 }
//             }
//         }
//         return req;
//     }

//     private static class BatchEvaluateRequest {
//         public List<Item> items = new ArrayList<>();

//         @lombok.Data
//         private static class Item {
//             public String job;
//             public String resumeBase64;
//             public String resumeFileName;
//             public String resumeId;
//             public String candidateId;
//             public String jobPostingId;
//             public String jobApplicationId;
//         }
//     }
// }



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

    private static final int BATCH_SIZE = 10;

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
                    List<ShortlistEvaluationResult> results =
                            callBatchApi(job, chunk);
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
        HttpEntity<MultiValueMap<String, Object>> request =
                new HttpEntity<>(body, headers);
        log.info("Calling batch API with {} resumes", applications.size());
        ResponseEntity<String> response =
                restTemplate.postForEntity(shortlistEvaluateBatchUrl, request, String.class);
        String responseBody = response.getBody();
        if (responseBody == null || responseBody.isBlank()) {
            throw new RuntimeException("Empty response from batch API");
        }
        Map<String, Object> parsed = objectMapper.readValue(responseBody, Map.class);
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) parsed.get("results");
        List<ShortlistEvaluationResult> finalResults = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            Map<String, Object> item = results.get(i);
            String resultJson = objectMapper.writeValueAsString(item.get("result"));
            ShortlistEvaluationResult saved =
                    shortlistEvaluationResultService.persistShortlistEvaluationResult(
                            resultJson,
                            candidateIds.get(i),
                            jobIds.get(i),
                            appIds.get(i),
                            resumeIds.get(i)
                    ).orElse(null);
            if (saved != null) finalResults.add(saved);
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