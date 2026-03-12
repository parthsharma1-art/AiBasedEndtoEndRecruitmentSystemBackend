package com.aibackend.AiBasedEndtoEndSystem.controller;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobPostings;
import com.aibackend.AiBasedEndtoEndSystem.service.CandidateService;
import com.aibackend.AiBasedEndtoEndSystem.service.JobApplicationService;
import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.GetExchange;

import java.time.Instant;
import java.util.List;

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
