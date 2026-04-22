package com.aibackend.AiBasedEndtoEndSystem.controller;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.aibackend.AiBasedEndtoEndSystem.controller.CompanyProfileController.JobPostingsRequest;
import com.aibackend.AiBasedEndtoEndSystem.controller.CompanyProfileController.JobPostingsResponse;
import com.aibackend.AiBasedEndtoEndSystem.dto.AiInterviewFullDetailDto;
import com.aibackend.AiBasedEndtoEndSystem.dto.JobApplicationGeneratedTestDto;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications;
import com.aibackend.AiBasedEndtoEndSystem.entity.ShortlistEvaluationResult;
import com.aibackend.AiBasedEndtoEndSystem.service.CompanyProfileService;
import com.aibackend.AiBasedEndtoEndSystem.service.JobPostingService;
import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/profile/job")
@Slf4j
public class JobPostingController {
    @Autowired
    private JobPostingService jobPostingService;
    @Autowired
    private CompanyProfileService companyProfileService;


    @PostMapping("/post")
    public JobPostingsResponse createJob(@RequestBody JobPostingsRequest request) {
        log.info("The job posting request is :{}", request);
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        log.info("Logged In user: {}", user);
        return jobPostingService.createJobPosting(user, request);
    }

    @DeleteMapping("/delete/{id}")
    public Boolean deleteById(@PathVariable String id) {
        log.info("Delete job by ID:{}", id);
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        return jobPostingService.deleteJobById(id, user);
    }

    @PutMapping("/update/{jobId}")
    public CompanyProfileController.JobPostingsResponse updateJob(@PathVariable String jobId,
            @RequestBody CompanyProfileController.JobPostingsRequest request) {
        log.info("The Update Job for the Id :{}", jobId);
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        return companyProfileService.updateJobRequest(user, jobId, request);

    }

    @GetMapping("/get/{jobId}")
    public CompanyProfileController.JobPostingsResponse getJobById(@PathVariable String jobId) {
        log.info("Getting Job for the Id :{}", jobId);
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        return jobPostingService.getJobDetailsById(user, jobId);

    }

    @GetMapping("/get/{jobId}/applications")
    public List<JobApplicationResponse> getJobApplications(@PathVariable String jobId) {
        log.info("Getting all applied candidates for the JOB ID :{}", jobId);
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        return jobPostingService.getAllJobApplications(user, jobId);

    }

    @GetMapping("/applications/under-review")
    public List<JobApplicationResponse> getUnderReviewApplications() {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        }
        return jobPostingService.getUnderReviewJobApplications(user);
    }

    @PostMapping("/applications/{jobApplicationId}/decision")
    public JobApplicationResponse decideJobApplication(
            @PathVariable String jobApplicationId,
            @RequestBody RecruiterApplicationDecisionRequest request) {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        }
        return jobPostingService.recruiterDecideJobApplication(user, jobApplicationId, request);
    }

    @GetMapping("/applications/{jobApplicationId}/shortlist-evaluation")
    public ShortlistEvaluationWithJobResponse getShortlistEvaluationForJobApplication(
            @PathVariable String jobApplicationId) {
        log.info("Getting shortlist evaluation for job application {}", jobApplicationId);
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        }
        return jobPostingService.getShortlistEvaluationForJobApplication(user, jobApplicationId);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShortlistEvaluationWithJobResponse {
        private ShortlistEvaluationResult shortlistEvaluation;
        private CompanyProfileController.JobPostingsResponse jobPosting;
        private JobApplicationGeneratedTestDto generatedTest;
        private AiInterviewFullDetailDto aiInterview;
    }

    @Data
    public static class JobApplicationResponse {
        private String id;
        private String profileImageId;
        private String candidateName;
        private String candidateEmail;
        private String resumeId;
        private Instant applyDate;
        private JobApplications.JobStatus status;
        private String candidateId;
        private String jobId;
        private String jobTitle;
        private double atsScore;
        private List<String> candidateSkills;
    }

    @Data
    public static class RecruiterApplicationDecisionRequest {
        private JobApplications.JobStatus decision;
        private String message;
    }

}
