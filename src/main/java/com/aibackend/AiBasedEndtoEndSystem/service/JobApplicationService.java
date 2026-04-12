package com.aibackend.AiBasedEndtoEndSystem.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.aibackend.AiBasedEndtoEndSystem.controller.CandidateApplyJobController;
import com.aibackend.AiBasedEndtoEndSystem.controller.CompanyProfileController;
import com.aibackend.AiBasedEndtoEndSystem.controller.JobPostingController;
import com.aibackend.AiBasedEndtoEndSystem.dto.CandidateDashboardResponse;
import com.aibackend.AiBasedEndtoEndSystem.dto.AiGeneratedTestPayload;
import com.aibackend.AiBasedEndtoEndSystem.dto.CodingQuestion;
import com.aibackend.AiBasedEndtoEndSystem.dto.CodingQuestionSafeResponse;
import com.aibackend.AiBasedEndtoEndSystem.dto.JobPostingTestRequest;
import com.aibackend.AiBasedEndtoEndSystem.dto.McqQuestion;
import com.aibackend.AiBasedEndtoEndSystem.dto.McqQuestionSafeResponse;
import com.aibackend.AiBasedEndtoEndSystem.dto.StartTestResultResponse;
import com.aibackend.AiBasedEndtoEndSystem.dto.StartTestResultSafeResponse;
import com.aibackend.AiBasedEndtoEndSystem.dto.TestAnswerSubmissionRequest;
import com.aibackend.AiBasedEndtoEndSystem.dto.TestEvaluationResponse;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.Candidate;
import com.aibackend.AiBasedEndtoEndSystem.entity.CompanyProfile;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplicationGeneratedTest;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications.AIShortlistStatus;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications.JobStatus;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobPostings;
import com.aibackend.AiBasedEndtoEndSystem.entity.ShortlistEvaluationResult;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.repository.JobApplicationGeneratedTestRepository;
import com.aibackend.AiBasedEndtoEndSystem.repository.JobApplicationRepository;
import com.aibackend.AiBasedEndtoEndSystem.util.UniqueUtility;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class JobApplicationService {
    @Autowired
    private JobApplicationRepository repository;
    @Autowired
    private CompanyProfileService companyProfileService;
    @Autowired
    private CandidateService candidateService;
    @Autowired
    @Lazy
    private JobPostingService jobPostingService;
    @Autowired
    private UniqueUtility uniqueUtility;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ShortlistEvaluationResultService shortlistEvaluationResultService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JobApplicationGeneratedTestRepository jobApplicationGeneratedTestRepository;
    @Autowired
    private AiResumeEvaluatingService aiResumeEvaluatingService;
    @Autowired
    private BrevoEmailService brevoEmailService;

    @Value("${ai.test.generate.url}")
    private String testGenerateUrl;

    public Boolean createNewJobApplications(UserDTO user, CandidateApplyJobController.ApplyJobRequest request,
            String jobId, MultipartFile resume) {
        log.info("Creating new Job Application for the user :{}", user);
        if (ObjectUtils.isEmpty(request.getUseSameResume())) {
            throw new BadException("Resume value is required");
        }
        if (!request.getUseSameResume() && ObjectUtils.isEmpty(resume)) {
            throw new BadException("Resume is required");
        }
        if (ObjectUtils.isEmpty(request.getMobileNumber())) {
            throw new BadException("Mobile Number is required");
        }
        if (!request.getUseSameEmail() && ObjectUtils.isEmpty(request.getEmail())) {
            throw new BadException("Email is required");
        }
        Candidate candidate = candidateService.getCandidateById(user.getId());
        if (ObjectUtils.isEmpty(candidate)) {
            throw new BadException("Candidate not found for the ID :" + user.getId());
        }
        JobPostings jobPostings = jobPostingService.getJobPostingById(jobId);
        if (ObjectUtils.isEmpty(jobPostings)) {
            throw new BadException("Job not found for the id " + jobId);
        }
        Optional<JobApplications> jobApplications = repository.findByCandidateIdAndJobId(candidate.getId(),
                jobPostings.getId());
        if (jobApplications.isPresent()) {
            throw new BadException("Already Applied");
        }
        CompanyProfile company = companyProfileService.getCompanyProfileById(jobPostings.getCompanyId());
        if (ObjectUtils.isEmpty(company)) {
            throw new BadException("Company Profile not found " + jobPostings.getCompanyId());
        }
        JobApplications application = new JobApplications();
        application.setId(uniqueUtility.getNextNumber("JOB_APPLICATION", "job_application"));
        application.setJobId(jobId);
        application.setCandidateId(candidate.getId());
        application.setRecruiterId(jobPostings.getPostBy());
        application.setCompanyId(jobPostings.getCompanyId());
        application.setStatus(JobApplications.JobStatus.APPLIED);
        application.setAppliedAt(Instant.now());
        application.setCreatedAt(Instant.now());
        application.setCreatedBy(candidate.getId());
        application.setUpdatedAt(Instant.now());
        application.setCandidateName(candidate.getName());
        if (request.getUseSameEmail()) {
            application.setCandidateEmail(candidate.getEmail());
        } else {
            application.setCandidateEmail(request.getEmail());
        }
        application.setCompanyName(company.getBasicSetting().getCompanyName());
        application.setMobileNumber(request.getMobileNumber());
        application.setUpdatedBy(candidate.getId());
        if (!request.getUseSameResume()) {
            String resumeId = fileStorageService.storeFile(resume);
            application.setResumeId(resumeId);
        } else {
            application.setResumeId(candidate.getResumeId());
        }
        notificationService.createJobNotification(candidate, "Your application has been submitted successfully.",
                jobPostings);
        application = saveJobApplication(application);
        log.info("Saved Job applications :{}", application);

        if (!ObjectUtils.isEmpty(application.getResumeId()) && !application.getResumeId().isBlank() && jobPostings.isAssessmentRequired()) {
            aiResumeEvaluatingService.sendJobPostingAndResumeToShortlistEvaluate(
                    jobPostings,
                    application.getResumeId(),
                    candidate.getId(),
                    application.getId());
        }

        return Boolean.TRUE;

    }

    public JobApplications saveJobApplication(JobApplications jobApplications) {
        log.info("Saving job application for the ID :{}", jobApplications.getId());
        return repository.save(jobApplications);
    }

    public List<JobPostingController.JobApplicationResponse> getAllJobApplications(JobPostings jobPostings) {
        log.info("Getting all job applications for the ID :{}", jobPostings.getId());
        List<JobApplications> jobApplications = repository.findByJobId(jobPostings.getId());
        if (jobApplications.isEmpty()) {
            return null;
        }
        List<JobPostingController.JobApplicationResponse> jobApplicationResponses = new ArrayList<>();
        for (JobApplications applications : jobApplications) {
            ShortlistEvaluationResult shortlistEvaluationResult = shortlistEvaluationResultService
                    .getShortlistEvaluationForJobApplication(applications.getId());
            Candidate candidate = candidateService.getCandidateById(applications.getCandidateId());
            JobPostingController.JobApplicationResponse response = toJobApplicationResponse(applications, candidate);
            if (!ObjectUtils.isEmpty(shortlistEvaluationResult)) {
                response.setAtsScore(shortlistEvaluationResult.getScore());
            }
            jobApplicationResponses.add(response);
        }
        return jobApplicationResponses;

    }

    public List<JobApplications> getAllJobApplicationsDetails(JobPostings jobPostings) {
        log.info("Getting all job applications for the ID :{}", jobPostings.getId());
        List<JobApplications> jobApplications = repository.findByJobId(jobPostings.getId());
        if (jobApplications.isEmpty()) {
            return null;
        }
        return jobApplications;

    }

    public List<JobApplications> getAllJobApplicationsDetailsByStatusApplied(JobPostings jobPostings,
            JobApplications.JobStatus status) {
        log.info("Getting all job applications for the ID :{} and status :{}", jobPostings.getId(), status);
        List<JobApplications> jobApplications = repository.findByJobIdAndStatus(jobPostings.getId(), status);
        if (jobApplications.isEmpty()) {
            return Collections.emptyList();
        }
        return jobApplications;
    }

    public List<JobApplications> getApplicationsForProcessing() {
        Instant cutoffTime = Instant.now().minus(1, ChronoUnit.MINUTES);
        return repository.findByStatusAndCreatedAtBefore(
                JobApplications.JobStatus.UNDER_REVIEW,
                cutoffTime);
    }

    public JobPostingController.JobApplicationResponse toJobApplicationResponse(JobApplications jobApplications,
            Candidate candidate) {
        JobPostingController.JobApplicationResponse response = new JobPostingController.JobApplicationResponse();
        response.setId(jobApplications.getId());
        response.setStatus(jobApplications.getStatus());
        response.setCandidateId(jobApplications.getCandidateId());
        response.setApplyDate(jobApplications.getAppliedAt());
        response.setCandidateName(jobApplications.getCandidateName());
        response.setResumeId(jobApplications.getResumeId());
        response.setCandidateSkills(candidate.getSkills());
        response.setProfileImageId(candidate.getProfileImageId());
        return response;
    }

    public List<CandidateApplyJobController.CandidateAppliedJobResponse> getAllAppliedJobsForCandidate(
            Candidate candidate) {
        log.info("Get all applied jobs for the candidate :{}", candidate.getId());
        List<JobApplications> applications = repository.findByCandidateId(candidate.getId());
        List<CandidateApplyJobController.CandidateAppliedJobResponse> list = new ArrayList<>();
        for (JobApplications jobApplications : applications) {
            CandidateApplyJobController.CandidateAppliedJobResponse response = new CandidateApplyJobController.CandidateAppliedJobResponse();
            response.setId(jobApplications.getId());
            response.setCandidateId(jobApplications.getCandidateId());
            response.setCandidateEmail(jobApplications.getCandidateEmail());
            response.setAppliedAt(jobApplications.getAppliedAt());
            JobPostings jobPostings = jobPostingService.getJobPostingById(jobApplications.getJobId());
            CompanyProfile companyProfile = companyProfileService.getCompanyProfileById(jobPostings.getCompanyId());
            if (ObjectUtils.isEmpty(companyProfile)) {
                throw new BadException("Company profile not found for the id " + jobPostings.getCompanyId());
            }
            response.setJobId(jobApplications.getJobId());
            response.setResumeId(jobApplications.getResumeId());
            response.setJobProfile(jobPostings.getProfile());
            response.setJobType(jobPostings.getJobType());
            response.setTitle(jobPostings.getTitle());
            response.setSalaryRange(jobPostings.getSalaryRange());
            response.setCompanyName(companyProfile.getBasicSetting().getCompanyName() != null ? companyProfile.getBasicSetting().getCompanyName() : "");
            response.setCandidateMobileNumber(jobApplications.getMobileNumber());
            response.setJobStatus(jobApplications.getStatus());
            list.add(response);

        }
        return list;
    }

    public Integer totalJobAppliedCandidate(String jobId) {
        log.info("get number of applicants for the job ID :{}", jobId);
        List<JobApplications> applications = repository.findByJobId(jobId);
        return applications.size();
    }

    public JobApplications getJobApplicationById(String id) {
        log.info("Get job application by ID :{}", id);
        return repository.findById(id).orElse(null);
    }

    /**
     * Latest shortlist evaluation + job posting for this application (candidate
     * must own the application).
     */
    public JobPostingController.ShortlistEvaluationWithJobResponse getShortlistEvaluationForOwnApplication(
            UserDTO user, String jobApplicationId) {
        log.info("Get shortlist evaluation for candidate's job application {}", jobApplicationId);
        if (ObjectUtils.isEmpty(user) || ObjectUtils.isEmpty(user.getId())) {
            throw new BadException("User is required");
        }
        Candidate candidate = candidateService.getCandidateById(user.getId());
        if (ObjectUtils.isEmpty(candidate)) {
            throw new BadException("Candidate not found for the ID :" + user.getId());
        }
        JobApplications application = getJobApplicationById(jobApplicationId);
        if (ObjectUtils.isEmpty(application)) {
            throw new BadException("Job application not found: " + jobApplicationId);
        }
        if (!application.getCandidateId().equals(candidate.getId())) {
            throw new BadException("Unauthorized access to this job application");
        }
        ShortlistEvaluationResult evaluation = shortlistEvaluationResultService
                .getShortlistEvaluationForJobApplication(jobApplicationId);
        if (evaluation == null) {
            return null;
        }
        JobPostings job = jobPostingService.getJobPostingById(application.getJobId());
        if (ObjectUtils.isEmpty(job)) {
            throw new BadException("Job not found for the id " + application.getJobId());
        }
        return new JobPostingController.ShortlistEvaluationWithJobResponse(
                evaluation, new CompanyProfileController.JobPostingsResponse(job), null);
    }

    public StartTestResultSafeResponse startTestForJobApplication(UserDTO user, String jobApplicationId) {
        log.info("Start test for the user :{} and Job application ID :{}", user, jobApplicationId);
        if (ObjectUtils.isEmpty(user) || ObjectUtils.isEmpty(user.getId())) {
            throw new BadException("User is required");
        }
        Candidate candidate = candidateService.getCandidateById(user.getId());
        if (ObjectUtils.isEmpty(candidate)) {
            throw new BadException("Candidate not found for the ID :" + user.getId());
        }
        JobApplications jobApplications = getJobApplicationById(jobApplicationId);
        if (ObjectUtils.isEmpty(jobApplications)) {
            throw new BadException("Job application not found: " + jobApplicationId);
        }
        if (!jobApplications.getCandidateId().equals(candidate.getId())) {
            throw new BadException("Unauthorize access to start test for the candidate Id :" + candidate.getId());
        }
        if (!JobApplications.JobStatus.TEST_SCHEDULED.equals(jobApplications.getStatus())) {
            throw new BadException("Test can only be started for shortlisted applications");
        }
        JobPostings job = jobPostingService.getJobPostingById(jobApplications.getJobId());
        log.info("Job posted is :{} and job application is :{}", job, jobApplications);
        if (ObjectUtils.isEmpty(job)) {
            throw new BadException("Job not found for the id " + jobApplications.getJobId());
        }

        Optional<JobApplicationGeneratedTest> existingGenerated = jobApplicationGeneratedTestRepository
                .findByJobApplicationId(jobApplicationId);
        if (existingGenerated.isPresent() && hasGeneratedTestContent(existingGenerated.get())) {
            JobApplicationGeneratedTest doc = existingGenerated.get();
            log.info(
                    "Reusing existing generated test id={} for jobApplicationId={} (mcqs={}, coding={})",
                    doc.getId(),
                    jobApplicationId,
                    doc.getMcqs() != null ? doc.getMcqs().size() : 0,
                    doc.getCodingQuestions() != null ? doc.getCodingQuestions().size() : 0);
            return toSafeResponse(toStartTestResultResponse(doc));
        }

        JobPostingTestRequest body = toJobPostingTestRequest(job);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<JobPostingTestRequest> entity = new HttpEntity<>(body, headers);

        try {
            log.info("Calling test generate API: {}", testGenerateUrl);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(testGenerateUrl, entity, JsonNode.class);
            JsonNode result = response.getBody();
            if (result == null || result.isNull()) {
                throw new BadException("Test generation service returned an empty response");
            }

            AiGeneratedTestPayload payload;
            try {
                payload = objectMapper.convertValue(result, AiGeneratedTestPayload.class);
                log.info("AiGeneratedTestPayload :{}", payload);
            } catch (IllegalArgumentException ex) {
                log.error("Could not parse AI test JSON: {}", ex.getMessage());
                throw new BadException("Invalid test format returned from AI service");
            }
            if (payload == null) {
                throw new BadException("Invalid test format returned from AI service");
            }

            JobApplicationGeneratedTest saved = upsertGeneratedTest(
                    jobApplicationId, candidate.getId(), job.getId(), payload);
            log.info(
                    "Saved generated test id={} for jobApplicationId={} (mcqs={}, coding={})",
                    saved.getId(),
                    jobApplicationId,
                    saved.getMcqs() != null ? saved.getMcqs().size() : 0,
                    saved.getCodingQuestions() != null ? saved.getCodingQuestions().size() : 0);
            StartTestResultResponse full = toStartTestResultResponse(saved);
            return toSafeResponse(full);
        } catch (RestClientException e) {
            log.error("Test generate API failed: {}", e.getMessage());
            throw new BadException("Failed to generate test: " + e.getMessage());
        }
    }

    /**
     * Returns the persisted generated test (MCQs with correctAnswer, full coding
     * questions) without calling the AI service.
     */
    public StartTestResultResponse getSavedGeneratedTestForJobApplication(UserDTO user, String jobApplicationId) {
        log.info("Get saved generated test for user :{} jobApplicationId :{}", user, jobApplicationId);
        if (ObjectUtils.isEmpty(user) || ObjectUtils.isEmpty(user.getId())) {
            throw new BadException("User is required");
        }
        Candidate candidate = candidateService.getCandidateById(user.getId());
        if (ObjectUtils.isEmpty(candidate)) {
            throw new BadException("Candidate not found for the ID :" + user.getId());
        }
        JobApplications jobApplications = getJobApplicationById(jobApplicationId);
        if (ObjectUtils.isEmpty(jobApplications)) {
            throw new BadException("Job application not found: " + jobApplicationId);
        }
        if (!jobApplications.getCandidateId().equals(candidate.getId())) {
            throw new BadException("Unauthorize access to start test for the candidate Id :" + candidate.getId());
        }
        if (!JobApplications.JobStatus.SHORTLISTED.equals(jobApplications.getStatus())) {
            throw new BadException("Test can only be started for shortlisted applications");
        }
        Optional<JobApplicationGeneratedTest> existing = jobApplicationGeneratedTestRepository
                .findByJobApplicationId(jobApplicationId);
        if (existing.isEmpty()) {
            throw new BadException("No generated test found for this application. Start the test first.");
        }
        return toStartTestResultResponse(existing.get());
    }

    public TestEvaluationResponse evaluateAndStoreTestAnswers(
            UserDTO user, String jobApplicationId, TestAnswerSubmissionRequest request) {
        log.info("Evaluate test answers jobApplicationId :{}", jobApplicationId);
        if (request == null) {
            throw new BadException("Request body is required");
        }
        if (ObjectUtils.isEmpty(user) || ObjectUtils.isEmpty(user.getId())) {
            throw new BadException("User is required");
        }
        Candidate candidate = candidateService.getCandidateById(user.getId());
        if (ObjectUtils.isEmpty(candidate)) {
            throw new BadException("Candidate not found for the ID :" + user.getId());
        }
        JobApplications jobApplications = getJobApplicationById(jobApplicationId);
        if (ObjectUtils.isEmpty(jobApplications)) {
            throw new BadException("Job application not found: " + jobApplicationId);
        }
        if (!jobApplications.getCandidateId().equals(candidate.getId())) {
            throw new BadException("Unauthorize access for the candidate Id :" + candidate.getId());
        }
        if (!JobApplications.JobStatus.TEST_SCHEDULED.equals(jobApplications.getStatus())) {
            throw new BadException("Answers can only be submitted for shortlisted applications");
        }
        JobApplicationGeneratedTest doc = jobApplicationGeneratedTestRepository
                .findByJobApplicationId(jobApplicationId)
                .orElseThrow(
                        () -> new BadException("No generated test found for this application. Start the test first."));

        List<McqQuestion> mcqs = doc.getMcqs() != null ? doc.getMcqs() : List.of();
        List<CodingQuestion> coding = doc.getCodingQuestions() != null ? doc.getCodingQuestions() : List.of();
        List<String> mcqAnswers = request.getMcqAnswers() != null ? request.getMcqAnswers() : List.of();
        List<String> codingAnswers = request.getCodingAnswers() != null ? request.getCodingAnswers() : List.of();

        if (mcqAnswers.size() != mcqs.size()) {
            throw new BadException("Expected " + mcqs.size() + " MCQ answers, got " + mcqAnswers.size());
        }
        if (codingAnswers.size() != coding.size()) {
            throw new BadException("Expected " + coding.size() + " coding answers, got " + codingAnswers.size());
        }

        List<Boolean> mcqResults = new ArrayList<>();
        for (int i = 0; i < mcqs.size(); i++) {
            mcqResults.add(evaluateMcqAnswer(mcqs.get(i), mcqAnswers.get(i)));
        }
        List<Boolean> codingResults = new ArrayList<>();
        for (int i = 0; i < coding.size(); i++) {
            codingResults.add(evaluateCodingAnswer(coding.get(i), codingAnswers.get(i)));
        }

        doc.setSubmittedMcqAnswers(new ArrayList<>(mcqAnswers));
        doc.setSubmittedCodingAnswers(new ArrayList<>(codingAnswers));
        doc.setMcqEvaluations(mcqResults);
        doc.setCodingEvaluations(codingResults);
        doc.setEvaluatedAt(Instant.now());
        jobApplicationGeneratedTestRepository.save(doc);

        int mcqCorrect = (int) mcqResults.stream().filter(Boolean::booleanValue).count();
        int codingCorrect = (int) codingResults.stream().filter(Boolean::booleanValue).count();
        int totalQuestions = mcqs.size() + coding.size();
        int totalCorrect = mcqCorrect + codingCorrect;
        double scorePercent = totalQuestions == 0 ? 0.0 : (totalCorrect * 100.0) / totalQuestions;

        // Auto-finalize application from test score:
        // 50% or above -> selected (HIRED), below 50% -> REJECTED.
        if (scorePercent >= 50.0) {
            jobApplications.setStatus(JobApplications.JobStatus.INTERVIEW_SCHEDULED);
            jobApplications.setAiShortlistStatus(JobApplications.AIShortlistStatus.SHORTLISTED);
        } else {
            jobApplications.setStatus(JobApplications.JobStatus.REJECTED);
            jobApplications.setAiShortlistStatus(JobApplications.AIShortlistStatus.REJECTED);
        }
        jobApplications.setUpdatedAt(Instant.now());
        jobApplications.setUpdatedBy(candidate.getId());
        saveJobApplication(jobApplications);
        log.info("Test evaluated for application {} with score {}%, status set to {}",
                jobApplicationId, scorePercent, jobApplications.getStatus());

        return TestEvaluationResponse.builder()
                .jobApplicationId(jobApplicationId)
                .mcqTotal(mcqs.size())
                .mcqCorrectCount(mcqCorrect)
                .codingTotal(coding.size())
                .codingCorrectCount(codingCorrect)
                .mcqPerQuestionCorrect(mcqResults)
                .codingPerQuestionCorrect(codingResults)
                .evaluatedAt(doc.getEvaluatedAt())
                .build();
    }

    private static boolean evaluateMcqAnswer(McqQuestion question, String submitted) {
        if (question == null) {
            return false;
        }
        String correct = question.getCorrectAnswer();
        if (ObjectUtils.isEmpty(correct)) {
            return false;
        }
        String normSub = normalizeMcqText(submitted);
        if (normSub.equals(normalizeMcqText(correct))) {
            return true;
        }
        String resolvedCorrectOption = resolveCorrectOptionText(question);
        if (!resolvedCorrectOption.isBlank()) {
            return normSub.equals(normalizeMcqText(resolvedCorrectOption));
        }
        return false;
    }

    private static String resolveCorrectOptionText(McqQuestion question) {
        List<String> options = question.getOptions();
        if (options == null || options.isEmpty()) {
            return "";
        }
        String correct = question.getCorrectAnswer();
        if (ObjectUtils.isEmpty(correct)) {
            return "";
        }
        String trimmed = correct.trim();
        try {
            int idx = Integer.parseInt(trimmed);
            // Support both 0-based and 1-based index formats from AI services.
            if (idx >= 0 && idx < options.size()) {
                return options.get(idx);
            }
            int oneBasedIdx = idx - 1;
            if (oneBasedIdx >= 0 && oneBasedIdx < options.size()) {
                return options.get(oneBasedIdx);
            }
        } catch (NumberFormatException ignored) {
            // correct_answer is not numeric; try alphabetical index format.
        }
        if (trimmed.length() == 1) {
            char ch = Character.toUpperCase(trimmed.charAt(0));
            if (ch >= 'A' && ch <= 'Z') {
                int idx = ch - 'A';
                if (idx >= 0 && idx < options.size()) {
                    return options.get(idx);
                }
            }
        }
        return "";
    }

    private static boolean evaluateCodingAnswer(CodingQuestion question, String submitted) {
        if (question == null) {
            return false;
        }
        String expected = question.getSampleOutput();
        if (ObjectUtils.isEmpty(expected)) {
            return false;
        }
        return normalizeCodingOutput(submitted).equals(normalizeCodingOutput(expected));
    }

    private static String normalizeMcqText(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeCodingOutput(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim().replace("\r\n", "\n").replace("\r", "\n");
        return t.lines().map(String::trim).collect(Collectors.joining("\n")).trim();
    }

    private JobApplicationGeneratedTest upsertGeneratedTest(
            String jobApplicationId,
            String candidateId,
            String jobId,
            AiGeneratedTestPayload payload) {
        Optional<JobApplicationGeneratedTest> existing = jobApplicationGeneratedTestRepository
                .findByJobApplicationId(jobApplicationId);
        JobApplicationGeneratedTest doc;
        if (existing.isPresent()) {
            doc = existing.get();
        } else {
            doc = new JobApplicationGeneratedTest();
            doc.setId(uniqueUtility.getNextNumber("JOB_APP_GEN_TEST", "job_app_gen_test"));
            doc.setJobApplicationId(jobApplicationId);
            doc.setCandidateId(candidateId);
            doc.setJobId(jobId);
        }
        doc.setCreatedAt(Instant.now());
        doc.setMcqs(payload.getMcqs() != null ? new ArrayList<>(payload.getMcqs()) : new ArrayList<>());
        doc.setCodingQuestions(
                payload.getCodingQuestions() != null ? new ArrayList<>(payload.getCodingQuestions())
                        : new ArrayList<>());
        return jobApplicationGeneratedTestRepository.save(doc);
    }

    private static StartTestResultSafeResponse toSafeResponse(StartTestResultResponse response) {
        List<McqQuestion> mcqs = response.getMcqs() != null ? response.getMcqs() : List.of();
        List<McqQuestionSafeResponse> mcqList = mcqs.stream()
                .map(mcq -> new McqQuestionSafeResponse(mcq.getQuestion(), mcq.getOptions()))
                .toList();

        List<CodingQuestion> coding = response.getCodingQuestions() != null
                ? response.getCodingQuestions()
                : List.of();
        List<CodingQuestionSafeResponse> codingList = coding.stream()
                .map(code -> new CodingQuestionSafeResponse(
                        code.getTitle(), code.getDescription(), code.getSampleOutput()))
                .toList();
        return new StartTestResultSafeResponse(
                response.getId(),
                response.getJobApplicationId(),
                response.getJobId(),
                response.getCandidateId(),
                response.getCreatedAt(),
                mcqList,
                codingList);
    }

    private static boolean hasGeneratedTestContent(JobApplicationGeneratedTest doc) {
        if (doc == null) {
            return false;
        }
        boolean hasMcq = doc.getMcqs() != null && !doc.getMcqs().isEmpty();
        boolean hasCoding = doc.getCodingQuestions() != null && !doc.getCodingQuestions().isEmpty();
        return hasMcq || hasCoding;
    }

    private static StartTestResultResponse toStartTestResultResponse(JobApplicationGeneratedTest doc) {
        return new StartTestResultResponse(
                doc.getId(),
                doc.getJobApplicationId(),
                doc.getJobId(),
                doc.getCandidateId(),
                doc.getCreatedAt(),
                doc.getMcqs(),
                doc.getCodingQuestions());
    }

    private static JobPostingTestRequest toJobPostingTestRequest(JobPostings job) {
        JobPostingTestRequest req = new JobPostingTestRequest();
        req.setTitle(job.getTitle());
        req.setDescription(job.getDescription() != null ? job.getDescription() : "");
        req.setSkillsRequired(
                job.getSkillsRequired() != null ? new ArrayList<>(job.getSkillsRequired()) : new ArrayList<>());
        req.setDifficulty("Intermediate");
        if (job.getExperienceRequired() != null) {
            req.setExperienceRequired(job.getExperienceRequired().doubleValue());
        }
        req.setProfile(job.getProfile());
        req.setJobType(job.getJobType() != null ? job.getJobType().name() : null);
        req.setSalaryRange(job.getSalaryRange());
        return req;
    }

    public Boolean rejectJobApplication(UserDTO userDTO, String jobApplicationId, String reason) {
        log.info("Reject job application {} for user {}", jobApplicationId, userDTO != null ? userDTO.getId() : null);
        if (ObjectUtils.isEmpty(userDTO) || ObjectUtils.isEmpty(userDTO.getId())) {
            throw new BadException("User is required");
        }
        JobApplications application = getJobApplicationById(jobApplicationId);
        if (ObjectUtils.isEmpty(application)) {
            throw new BadException("Job application not found: " + jobApplicationId);
        }
        Candidate candidate = candidateService.getCandidateById(application.getCandidateId());
        if (ObjectUtils.isEmpty(candidate)) {
            throw new BadException("Candidate not found for the ID :" + application.getCandidateId());
        }
        JobPostings job = jobPostingService.getJobPostingById(application.getJobId());
        if (ObjectUtils.isEmpty(job)) {
            throw new BadException("Job not found for the id " + application.getJobId());
        }
        application.setStatus(JobApplications.JobStatus.REJECTED);
        application.setAiShortlistStatus(JobApplications.AIShortlistStatus.REJECTED);
        application.setRejectReason(reason);
        application.setUpdatedAt(Instant.now());
        application.setUpdatedBy(candidate.getId());
        saveJobApplication(application);
        log.info("Job application {} marked REJECTED (candidate {})", jobApplicationId, candidate.getId());
        return Boolean.TRUE;
    }

    public JobApplications updateJobApplicationStatusIfShortlisted(Boolean shortlisted, String jobApplicationId) {
        if (jobApplicationId == null || jobApplicationId.isBlank()) {
            return null;
        }
        log.info("shortlisted value :{} and job application Id :{}", shortlisted, jobApplicationId);
        JobApplications jobApplications = repository.findById(jobApplicationId).orElse(null);
        if (jobApplications == null) {
            log.warn("Job application {} not found; cannot update status after AI shortlist", jobApplicationId);
            return null;
        }
        if (Boolean.TRUE.equals(shortlisted)) {
            jobApplications.setAiShortlistStatus(AIShortlistStatus.SHORTLISTED);
        } else {
            jobApplications.setAiShortlistStatus(AIShortlistStatus.REJECTED);
        }
        jobApplications.setStatus(JobApplications.JobStatus.UNDER_REVIEW);
        jobApplications.setUpdatedAt(Instant.now());
        JobApplications saved = saveJobApplication(jobApplications);
        log.info("Job application {} status updated after AI shortlist (shortlisted={})", jobApplicationId,
                shortlisted);
        return saved;
    }

    public void updateJobApplicationAndSendNotification(JobApplications jobApplications, JobStatus jobStatus) {
        jobApplications.setStatus(jobStatus);
        jobApplications.setUpdatedAt(Instant.now());
        saveJobApplication(jobApplications);
        ShortlistEvaluationResult shortlistEvaluationResult = shortlistEvaluationResultService
                .getShortlistEvaluationForJobApplication(jobApplications.getId());
        sendShortlistNotificationEmail(jobApplications, shortlistEvaluationResult);

    }

    private void sendShortlistNotificationEmail(JobApplications application, ShortlistEvaluationResult evaluation) {
        if (application == null || evaluation == null) {
            return;
        }
        String jobTitle = Optional.ofNullable(jobPostingService.getJobPostingById(application.getJobId()))
                .map(JobPostings::getTitle)
                .filter(t -> t != null && !t.isBlank())
                .orElse("this position");
        String companyName = application.getCompanyName() != null && !application.getCompanyName().isBlank()
                ? application.getCompanyName()
                : "the company";
        String candidateName = application.getCandidateName() != null && !application.getCandidateName().isBlank()
                ? application.getCandidateName()
                : "Candidate";
        boolean shortlisted = Boolean.TRUE.equals(evaluation.getShortlisted());
        brevoEmailService.sendApplicationShortlistResultEmail(
                application.getCandidateEmail(),
                candidateName,
                jobTitle,
                companyName,
                shortlisted,
                evaluation.getScore());

        Candidate candidate = candidateService.getCandidateById(application.getCandidateId());
        JobPostings job = jobPostingService.getJobPostingById(application.getJobId());
        if (candidate != null && job != null) {
            notificationService.createAiScreeningResultNotification(
                    candidate, job, shortlisted, application.getId());
        }
    }


    public List<JobApplications> getAllJobApplicationsDetailsByCandidateId(String candidateId) {
        List<JobApplications> jobApplications = repository.findByCandidateId(candidateId);
        if (jobApplications.isEmpty()) {
            return Collections.emptyList();
        }
        return jobApplications;
    }

    public CandidateDashboardResponse.ApplicationSummary buildApplicationSummary(String candidateId) {
        List<JobApplications> apps = repository.findByCandidateId(candidateId);
        CandidateDashboardResponse.ApplicationSummary summary = new CandidateDashboardResponse.ApplicationSummary();
        summary.setTotalApplications(apps.size());
        summary.setShortlisted((int) apps.stream().filter(a -> a.getStatus() == JobStatus.SHORTLISTED).count());
        summary.setUnderReview((int) apps.stream().filter(a -> a.getStatus() == JobStatus.UNDER_REVIEW).count());
        summary.setRejected((int) apps.stream().filter(a -> a.getStatus() == JobStatus.REJECTED).count());
        summary.setApplied((int) apps.stream().filter(a -> a.getStatus() == JobStatus.APPLIED).count());
        summary.setTestScheduled((int) apps.stream().filter(a -> a.getStatus() == JobStatus.TEST_SCHEDULED).count());
        return summary;
    }

}
