package com.aibackend.AiBasedEndtoEndSystem.controller;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.aibackend.AiBasedEndtoEndSystem.dto.StartTestResultSafeResponse;
import com.aibackend.AiBasedEndtoEndSystem.dto.TestAnswerSubmissionRequest;
import com.aibackend.AiBasedEndtoEndSystem.dto.TestEvaluationResponse;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobPostings;
import com.aibackend.AiBasedEndtoEndSystem.controller.JobPostingController.ShortlistEvaluationWithJobResponse;
import com.aibackend.AiBasedEndtoEndSystem.service.CandidateService;
import com.aibackend.AiBasedEndtoEndSystem.service.JobApplicationService;
import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;

import lombok.Data;

@RestController
@RequestMapping("/jobs")
public class CandidateApplyJobController {

    @Autowired
    private JobApplicationService jobApplicationService;
    @Autowired
    private CandidateService candidateService;

    @PostMapping(value = "/{jobId}/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Boolean createNewHR(
            @ModelAttribute ApplyJobRequest request, @PathVariable String jobId,
            @RequestPart(value = "resume", required = false) MultipartFile resume) {
        UserDTO userDTO = SecurityUtils.getLoggedInUser();
        return jobApplicationService.createNewJobApplications(userDTO, request, jobId, resume);
    }

    @GetMapping("/applied")
    public List<CandidateAppliedJobResponse> getAllAppliedJobs() {
        UserDTO userDTO = SecurityUtils.getLoggedInUser();
        return candidateService.getAllAppliedJobs(userDTO);
    }

    @PostMapping("/applied/{jobApplicationId}/shortlisted/start")
    public StartTestResultSafeResponse startTest(@PathVariable String jobApplicationId) {
        UserDTO userDTO = SecurityUtils.getLoggedInUser();
        if (userDTO == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        }
        return jobApplicationService.startTestForJobApplication(userDTO, jobApplicationId);
    }

    /**
     * Same payload as {@code GET /api/profile/job/applications/{id}/shortlist-evaluation}, for the candidate who owns
     * the application.
     */
    @GetMapping("/applied/{jobApplicationId}/shortlist-evaluation")
    public ShortlistEvaluationWithJobResponse getShortlistEvaluationForMyApplication(
            @PathVariable String jobApplicationId) {
        UserDTO userDTO = SecurityUtils.getLoggedInUser();
        if (userDTO == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        }
        ShortlistEvaluationWithJobResponse result =
                jobApplicationService.getShortlistEvaluationForOwnApplication(userDTO, jobApplicationId);
        if (result == null) {
            throw new ResponseStatusException(NOT_FOUND, "No shortlist evaluation found for this job application");
        }
        return result;
    }

    @PostMapping(value = "/applied/{jobApplicationId}/shortlisted/answer-evaluation", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TestEvaluationResponse submitAndEvaluateAnswers(
            @PathVariable String jobApplicationId, @RequestBody TestAnswerSubmissionRequest body) {
        UserDTO userDTO = SecurityUtils.getLoggedInUser();
        if (userDTO == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        }
        return jobApplicationService.evaluateAndStoreTestAnswers(userDTO, jobApplicationId, body);
    }

    @PostMapping("/applied/{jobApplicationId}/reject")
    public Boolean rejectCandidate(@PathVariable String jobApplicationId) {
        UserDTO userDTO = SecurityUtils.getLoggedInUser();
        if (userDTO == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        }
        return jobApplicationService.rejectJobApplication(userDTO, jobApplicationId);
        
    }

    @Data
    public static class ApplyJobRequest {
        private Boolean useSameResume;
        private Boolean useSameEmail;
        private String email;
        private String mobileNumber;
    }

    @Data
    public static class CandidateAppliedJobResponse {
        private String id;
        private String candidateId;
        private String jobId;
        private String resumeId;
        private Instant appliedAt;
        private String jobProfile;
        private String candidateEmail;
        private String candidateMobileNumber;
        private String salaryRange;
        private String title;
        private JobPostings.JobType jobType;
        private String companyName;
        private JobApplications.JobStatus jobStatus;
    }
}
