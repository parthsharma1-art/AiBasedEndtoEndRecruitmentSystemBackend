package com.aibackend.AiBasedEndtoEndSystem.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.aibackend.AiBasedEndtoEndSystem.dto.AiInterviewApiResponse;
import com.aibackend.AiBasedEndtoEndSystem.dto.AiInterviewFullDetailDto;
import com.aibackend.AiBasedEndtoEndSystem.dto.AiInterviewJobSummaryDto;
import com.aibackend.AiBasedEndtoEndSystem.dto.AiInterviewSummaryResponse;
import com.aibackend.AiBasedEndtoEndSystem.dto.AiInterviewTurnOutDto;
import com.aibackend.AiBasedEndtoEndSystem.entity.AiInterviewSession;
import com.aibackend.AiBasedEndtoEndSystem.entity.AiInterviewSession.InterviewSessionStatus;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications.JobStatus;
import com.aibackend.AiBasedEndtoEndSystem.entity.AiInterviewTurn;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobPostings;
import com.aibackend.AiBasedEndtoEndSystem.entity.SalaryRangeLpa;
import com.aibackend.AiBasedEndtoEndSystem.repository.AiInterviewSessionRepository;
import com.aibackend.AiBasedEndtoEndSystem.repository.JobApplicationRepository;
import com.aibackend.AiBasedEndtoEndSystem.repository.JobPostingRepository;
import com.aibackend.AiBasedEndtoEndSystem.util.UniqueUtility;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiInterviewService {

    private final RestTemplate restTemplate;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final AiInterviewSessionRepository aiInterviewSessionRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final UniqueUtility uniqueUtility;

    @Value("${ai.interview.start.url}")
    private String interviewStartUrl;

    @Value("${ai.interview.answer.url}")
    private String interviewAnswerUrl;

    public AiInterviewApiResponse startInterview(JobPostings job, JobApplications application) {
        if (application.getResumeId() == null || application.getResumeId().isBlank()) {
            throw new IllegalArgumentException("Resume is missing for this job application");
        }
        GridFsResource resumeResource = fileStorageService.getFile(application.getResumeId());
        final byte[] resumeBytes;
        final String filename;
        try {
            resumeBytes = resumeResource.getInputStream().readAllBytes();
            filename = resumeResource.getFilename() != null && !resumeResource.getFilename().isBlank()
                    ? resumeResource.getFilename()
                    : "resume.pdf";
        } catch (IOException e) {
            log.error("Failed to read resume from GridFS: {}", e.getMessage());
            throw new IllegalStateException("Could not read resume file", e);
        }

        String jobJson = buildFullJobJson(job);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("job", jobJson);
        body.add("resume", new ByteArrayResource(resumeBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        body.add("candidate_id", application.getCandidateId());
        body.add("job_posting_id", job.getId());
        body.add("job_application_id", application.getId());
        body.add("resume_id", application.getResumeId());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            log.info("Calling interview start API for jobApplicationId={}", application.getId());
            ResponseEntity<String> response = restTemplate.postForEntity(interviewStartUrl, request, String.class);
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                throw new IllegalStateException("Interview service returned an empty body");
            }
            JsonNode root = objectMapper.readTree(responseBody);
            AiInterviewSession session = persistSessionAfterStart(job, application, responseBody, root);
            return buildApiResponse(job, application, session, root);
        } catch (HttpStatusCodeException e) {
            log.error("Interview start API HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException(summarizeAiError(e), e);
        } catch (RestClientException e) {
            log.error("Interview start API call failed: {}", e.getMessage());
            throw new IllegalStateException("Interview service request failed: " + e.getMessage(), e);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calling interview start: {}", e.getMessage());
            throw new IllegalStateException("Could not parse interview service response", e);
        }
    }

    /**
     * Sends the candidate's answer to the AI service and updates
     * {@code ai_interviews} with the
     * answer and the raw AI response (including the next question when present).
     */
    public AiInterviewApiResponse submitAnswer(JobPostings job, JobApplications application, String pythonSessionId,
            String answerText) {
        if (pythonSessionId == null || pythonSessionId.isBlank()) {
            throw new IllegalArgumentException("session_id is required");
        }
        if (answerText == null || answerText.isBlank()) {
            throw new IllegalArgumentException("answer must not be empty");
        }
        AiInterviewSession session = aiInterviewSessionRepository
                .findByPythonSessionIdAndJobApplicationId(pythonSessionId, application.getId())
                .orElseThrow(() -> new IllegalArgumentException("Interview session not found for this application"));
        if (!session.getCandidateId().equals(application.getCandidateId())) {
            throw new IllegalArgumentException("Interview session does not match this application");
        }
        if (session.getStatus() == InterviewSessionStatus.COMPLETED) {
            throw new IllegalStateException("This interview session is already completed");
        }
        AiInterviewTurn openTurn = findOpenTurn(session.getTurns());
        if (openTurn == null) {
            throw new IllegalStateException("There is no pending interview question to answer");
        }

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("session_id", pythonSessionId);
        payload.put("answer", answerText);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(interviewAnswerUrl, entity, String.class);
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.isBlank()) {
                throw new IllegalStateException("Interview answer API returned an empty body");
            }
            JsonNode resp = objectMapper.readTree(responseBody);
            Instant now = Instant.now();
            openTurn.setCandidateAnswer(answerText);
            openTurn.setCandidateAnsweredAt(now);
            openTurn.setPostAnswerAiResponseJson(objectMapper.writeValueAsString(resp));

            String nextQuestion = extractNextQuestionText(resp);
            boolean done = explicitInterviewDone(resp) || nextQuestion == null;

            if (done) {
                session.setStatus(InterviewSessionStatus.COMPLETED);
                applyCompletionScoresFromResponse(session, resp);
            } else {
                AiInterviewTurn next = new AiInterviewTurn();
                next.setTurnIndex(session.getTurns().size());
                next.setAiQuestion(nextQuestion);
                next.setQuestionRecordedAt(Instant.now());
                session.getTurns().add(next);
            }
            session.setUpdatedAt(now);
            session.setUpdatedBy(application.getCandidateId());
            AiInterviewSession saved = aiInterviewSessionRepository.save(session);
            application.setStatus(JobStatus.UNDER_RECRUITER_REVIEW);
            application.setUpdatedAt(Instant.now());
            jobApplicationRepository.save(application);
            return buildApiResponse(job, application, saved, resp);
        } catch (HttpStatusCodeException e) {
            log.error("Interview answer API HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException(summarizeAiError(e), e);
        } catch (RestClientException e) {
            log.error("Interview answer API call failed: {}", e.getMessage());
            throw new IllegalStateException("Interview service request failed: " + e.getMessage(), e);
        } catch (IllegalStateException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calling interview answer: {}", e.getMessage());
            throw new IllegalStateException("Could not process interview answer response", e);
        }
    }

    private AiInterviewSession persistSessionAfterStart(JobPostings job, JobApplications application,
            String rawResponseBody, JsonNode root) {
        String pythonSessionId = root.path("session_id").asText(null);
        if (pythonSessionId == null || pythonSessionId.isBlank()) {
            throw new IllegalStateException("Interview start response missing session_id");
        }
        String firstQuestion = root.path("question").asText("");
        Instant now = Instant.now();
        AiInterviewSession session = new AiInterviewSession();
        session.setId(uniqueUtility.getNextNumber("AI_INTERVIEW_SESSION", "ai_interview"));
        session.setPythonSessionId(pythonSessionId);
        session.setJobApplicationId(application.getId());
        session.setJobId(job.getId());
        session.setCandidateId(application.getCandidateId());
        session.setResumeId(application.getResumeId());
        session.setStatus(InterviewSessionStatus.IN_PROGRESS);
        session.setStartResponseJson(rawResponseBody);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        session.setCreatedBy(application.getCandidateId());
        session.setUpdatedBy(application.getCandidateId());

        AiInterviewTurn turn0 = new AiInterviewTurn();
        turn0.setTurnIndex(0);
        turn0.setAiQuestion(firstQuestion);
        turn0.setQuestionRecordedAt(now);
        session.setTurns(new ArrayList<>(List.of(turn0)));

        aiInterviewSessionRepository.save(session);
        log.info("Saved ai_interviews id={} pythonSessionId={} jobApplicationId={}", session.getId(), pythonSessionId,
                application.getId());
        return session;
    }

    /**
     * Latest interview session for this application (by {@code updatedAt}), with
     * completion scores
     * backfilled to the document when missing (legacy rows).
     */
    public AiInterviewSummaryResponse getInterviewSummary(JobPostings job, JobApplications application) {
        AiInterviewSession session = aiInterviewSessionRepository
                .findFirstByJobApplicationIdOrderByUpdatedAtDesc(application.getId())
                .orElseThrow(() -> new IllegalArgumentException("No AI interview session found for this application"));
        if (!session.getCandidateId().equals(application.getCandidateId())) {
            throw new IllegalArgumentException("Interview session does not belong to this candidate");
        }
        AiInterviewSession fresh = ensureCompletionMetadataPersisted(session);
        return buildSummaryResponse(job, application, fresh);
    }

    /**
     * Latest AI interview for this job application: summary (scores, status, job) plus all turns
     * with parsed score/feedback. Used by recruiter shortlist view; {@code null} if no session
     * exists.
     */
    public AiInterviewFullDetailDto getInterviewFullDetailForJobApplicationOrNull(String jobApplicationId) {
        return aiInterviewSessionRepository.findFirstByJobApplicationIdOrderByUpdatedAtDesc(jobApplicationId)
                .map(s -> buildInterviewFullDetail(jobApplicationId, s))
                .orElse(null);
    }

    private AiInterviewFullDetailDto buildInterviewFullDetail(String jobApplicationId, AiInterviewSession session) {
        AiInterviewSession fresh = ensureCompletionMetadataPersisted(session);
        JobApplications app = jobApplicationRepository.findById(jobApplicationId).orElse(null);
        if (app == null) {
            return null;
        }
        JobPostings job = jobPostingRepository.findById(fresh.getJobId()).orElse(null);
        if (job == null) {
            return null;
        }
        AiInterviewSummaryResponse summary = buildSummaryResponse(job, app, fresh);
        List<AiInterviewTurnOutDto> turns = mapTurnsToOut(fresh);
        return AiInterviewFullDetailDto.builder().summary(summary).turns(turns).build();
    }

    private List<AiInterviewTurnOutDto> mapTurnsToOut(AiInterviewSession session) {
        if (session.getTurns() == null || session.getTurns().isEmpty()) {
            return List.of();
        }
        List<AiInterviewTurnOutDto> out = new ArrayList<>();
        for (AiInterviewTurn t : session.getTurns()) {
            AiInterviewTurnOutDto.AiInterviewTurnOutDtoBuilder b = AiInterviewTurnOutDto.builder()
                    .turnIndex(t.getTurnIndex())
                    .aiQuestion(t.getAiQuestion())
                    .questionRecordedAt(t.getQuestionRecordedAt())
                    .candidateAnswer(t.getCandidateAnswer())
                    .candidateAnsweredAt(t.getCandidateAnsweredAt());
            String raw = t.getPostAnswerAiResponseJson();
            if (raw != null && !raw.isBlank()) {
                try {
                    JsonNode n = objectMapper.readTree(raw);
                    if (n.has("score") && n.get("score").isNumber()) {
                        b.score(n.get("score").asDouble());
                    }
                    if (n.has("feedback") && !n.get("feedback").isNull()) {
                        String fb = n.get("feedback").asText(null);
                        if (fb != null && !fb.isBlank()) {
                            b.feedback(fb);
                        }
                    }
                    for (String key : List.of("next_question", "nextQuestion", "question")) {
                        if (n.has(key) && !n.get(key).isNull()) {
                            String nq = n.get(key).asText("");
                            if (!nq.isBlank()) {
                                b.nextQuestionPreview(nq);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Skip parsing turn {} post-answer JSON: {}", t.getTurnIndex(), e.getMessage());
                }
            }
            out.add(b.build());
        }
        return out;
    }

    /**
     * All AI interview sessions for this candidate (newest
     * {@link AiInterviewSession#getUpdatedAt()}
     * first), with job/application context and completion scores backfilled when
     * needed.
     */
    public List<AiInterviewSummaryResponse> listInterviewSummariesForCandidate(String candidateId) {
        List<AiInterviewSession> sessions = aiInterviewSessionRepository
                .findByCandidateIdOrderByUpdatedAtDesc(candidateId);
        List<AiInterviewSummaryResponse> out = new ArrayList<>();
        for (AiInterviewSession session : sessions) {
            if (!candidateId.equals(session.getCandidateId())) {
                continue;
            }
            JobApplications application = jobApplicationRepository.findById(session.getJobApplicationId())
                    .orElse(null);
            if (application == null || !candidateId.equals(application.getCandidateId())) {
                continue;
            }
            JobPostings job = jobPostingRepository.findById(session.getJobId()).orElse(null);
            if (job == null) {
                continue;
            }
            AiInterviewSession fresh = ensureCompletionMetadataPersisted(session);
            out.add(buildSummaryResponse(job, application, fresh));
        }
        return out;
    }

    private AiInterviewSession ensureCompletionMetadataPersisted(AiInterviewSession session) {
        if (session.getStatus() != InterviewSessionStatus.COMPLETED || session.getOverallScore() != null) {
            return session;
        }
        String raw = lastTurnPostAnswerJson(session);
        if (raw == null || raw.isBlank()) {
            return session;
        }
        try {
            JsonNode n = objectMapper.readTree(raw);
            if (n.has("average_score") || n.has("overall_score") || n.has("detailed_evaluations")
                    || n.has("detailedEvaluations")) {
                applyCompletionScoresFromResponse(session, n);
                session.setUpdatedAt(Instant.now());
                return aiInterviewSessionRepository.save(session);
            }
        } catch (Exception e) {
            log.warn("Could not backfill interview scores for session {}: {}", session.getId(), e.getMessage());
        }
        return session;
    }

    private static String lastTurnPostAnswerJson(AiInterviewSession session) {
        List<AiInterviewTurn> turns = session.getTurns();
        if (turns == null || turns.isEmpty()) {
            return null;
        }
        AiInterviewTurn last = turns.get(turns.size() - 1);
        return last.getPostAnswerAiResponseJson();
    }

    private static void applyCompletionScoresFromResponse(AiInterviewSession session, JsonNode resp) {
        if (resp == null) {
            return;
        }
        if (resp.has("average_score") && resp.get("average_score").isNumber()) {
            session.setOverallScore(resp.get("average_score").asDouble());
        } else if (resp.has("overall_score") && resp.get("overall_score").isNumber()) {
            session.setOverallScore(resp.get("overall_score").asDouble());
        }
        if (resp.has("result") && !resp.get("result").isNull()) {
            String r = resp.get("result").asText(null);
            session.setInterviewResult(r != null && !r.isBlank() ? r : null);
        } else if (resp.has("interview_result") && !resp.get("interview_result").isNull()) {
            String r = resp.get("interview_result").asText(null);
            session.setInterviewResult(r != null && !r.isBlank() ? r : null);
        }
        JsonNode arr = resp.get("detailed_evaluations");
        if (arr == null || !arr.isArray()) {
            arr = resp.get("detailedEvaluations");
        }
        if (arr != null && arr.isArray()) {
            List<Double> scores = new ArrayList<>();
            for (JsonNode node : arr) {
                if (node.isNumber()) {
                    scores.add(node.asDouble());
                }
            }
            if (!scores.isEmpty()) {
                session.setDetailedEvaluationScores(scores);
            }
        }
    }

    private static AiInterviewSummaryResponse buildSummaryResponse(JobPostings job, JobApplications application,
            AiInterviewSession session) {
        return AiInterviewSummaryResponse.builder()
                .sessionId(session.getPythonSessionId())
                .backendSessionId(session.getId())
                .jobApplicationId(application.getId())
                .jobPostingId(job.getId())
                .candidateId(application.getCandidateId())
                .candidateName(application.getCandidateName())
                .resumeId(application.getResumeId())
                .status(session.getStatus())
                .overallScore(session.getOverallScore())
                .interviewResult(session.getInterviewResult())
                .detailedEvaluationScores(session.getDetailedEvaluationScores())
                .turnCount(session.getTurns() != null ? session.getTurns().size() : 0)
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .job(buildJobSummary(job, application))
                .build();
    }

    private AiInterviewApiResponse buildApiResponse(JobPostings job, JobApplications application,
            AiInterviewSession session,
            JsonNode lastAiJson) {
        boolean complete = session.getStatus() == InterviewSessionStatus.COMPLETED;
        String questionToShow = null;
        if (!complete) {
            AiInterviewTurn open = findOpenTurn(session.getTurns());
            if (open != null && open.getAiQuestion() != null && !open.getAiQuestion().isBlank()) {
                questionToShow = open.getAiQuestion();
            } else if (open != null) {
                questionToShow = "";
            }
        }
        String closing = null;
        if (complete) {
            closing = extractClosingMessage(lastAiJson);
            if (closing == null || closing.isBlank()) {
                String maybeFinal = extractNextQuestionText(lastAiJson);
                if (maybeFinal != null && !maybeFinal.isBlank()) {
                    closing = maybeFinal;
                }
            }
        }
        AiInterviewApiResponse.AiInterviewApiResponseBuilder b = AiInterviewApiResponse.builder()
                .sessionId(session.getPythonSessionId())
                .backendSessionId(session.getId())
                .jobApplicationId(application.getId())
                .jobPostingId(job.getId())
                .candidateId(application.getCandidateId())
                .candidateName(application.getCandidateName())
                .resumeId(application.getResumeId())
                .question(questionToShow)
                .interviewComplete(complete)
                .closingMessage(closing)
                .turnCount(session.getTurns() != null ? session.getTurns().size() : 0)
                .job(buildJobSummary(job, application));
        if (complete) {
            b.overallScore(session.getOverallScore())
                    .interviewResult(session.getInterviewResult())
                    .detailedEvaluationScores(session.getDetailedEvaluationScores());
        }
        return b.build();
    }

    private static AiInterviewJobSummaryDto buildJobSummary(JobPostings job, JobApplications application) {
        return AiInterviewJobSummaryDto.builder()
                .jobId(job.getId())
                .title(job.getTitle())
                .companyName(application.getCompanyName())
                .jobType(job.getJobType())
                .experienceRequired(job.getExperienceRequired())
                .profile(job.getProfile())
                .skillsRequired(job.getSkillsRequired())
                .build();
    }

    private static String extractClosingMessage(JsonNode resp) {
        if (resp == null) {
            return null;
        }
        for (String key : List.of("message", "farewell", "summary", "closing_message", "closingMessage", "closing")) {
            if (resp.has(key) && !resp.get(key).isNull()) {
                String text = resp.get(key).asText("").trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return null;
    }

    private static AiInterviewTurn findOpenTurn(List<AiInterviewTurn> turns) {
        if (turns == null || turns.isEmpty()) {
            return null;
        }
        for (int i = turns.size() - 1; i >= 0; i--) {
            AiInterviewTurn t = turns.get(i);
            if (t.getCandidateAnswer() == null || t.getCandidateAnswer().isBlank()) {
                return t;
            }
        }
        return null;
    }

    private static String extractNextQuestionText(JsonNode resp) {
        if (resp == null) {
            return null;
        }
        for (String key : List.of("question", "next_question", "nextQuestion")) {
            if (resp.has(key) && !resp.get(key).isNull()) {
                String text = resp.get(key).asText("");
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private static boolean explicitInterviewDone(JsonNode resp) {
        if (resp == null) {
            return true;
        }
        return resp.path("done").asBoolean(false)
                || resp.path("complete").asBoolean(false)
                || resp.path("finished").asBoolean(false);
    }

    private static String summarizeAiError(HttpStatusCodeException e) {
        String raw = e.getResponseBodyAsString();
        if (raw == null || raw.isBlank()) {
            return "Interview service error: HTTP " + e.getStatusCode().value();
        }
        return "Interview service error: " + raw;
    }

    @SuppressWarnings("deprecation")
    String buildFullJobJson(JobPostings job) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", job.getId());
            map.put("title", job.getTitle());
            map.put("description", job.getDescription());
            map.put("skillsRequired", job.getSkillsRequired());
            map.put("salaryRange", job.getSalaryRange());
            map.put("jobType", job.getJobType() != null ? job.getJobType().name() : null);
            map.put("experienceRequired", job.getExperienceRequired());
            map.put("profile", job.getProfile());
            map.put("locations", job.getLocations());
            map.put("postBy", job.getPostBy());
            map.put("isAssessmentRequired", job.isAssessmentRequired());
            map.put("isInterviewRequired", job.isInterviewRequired());
            SalaryRangeLpa lpa = job.getSalaryRangeInLPA();
            if (lpa != null) {
                Map<String, Object> lpaMap = new LinkedHashMap<>();
                lpaMap.put("min", lpa.getMin());
                lpaMap.put("max", lpa.getMax());
                map.put("salaryRangeInLPA", lpaMap);
            } else {
                map.put("salaryRangeInLPA", null);
            }
            map.put("shortlistPercentage", job.getShortlistPercentage());
            map.put("currency", job.getCurrency());
            map.put("companyId", job.getCompanyId());
            map.put("isActive", job.isActive());
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException("Error building job JSON for interview", e);
        }
    }
}
