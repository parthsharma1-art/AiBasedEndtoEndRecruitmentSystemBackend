package com.aibackend.AiBasedEndtoEndSystem.controller;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.CompanyProfile.SocialLinks;
import com.aibackend.AiBasedEndtoEndSystem.entity.CompanyProfile.ContactDetails;
import com.aibackend.AiBasedEndtoEndSystem.entity.CompanyProfile.BasicSetting;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobPostings;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.service.CompanyProfileService;
import com.aibackend.AiBasedEndtoEndSystem.service.JobPostingService;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/profile")
public class CompanyProfileController {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private CompanyProfileService companyProfileService;
    @Autowired
    private JobPostingService jobPostingService;

    @PutMapping("/update")
    public CompanyProfileResponse updateCompanyProfile(@RequestBody CompanyProfileRequest request, @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7); // remove "Bearer "
        String id = jwtUtil.extractUserObjectId(token);
        UserDTO userDTO = SecurityUtils.getLoggedInUser(token, jwtUtil.getKey());
        return companyProfileService.updateCompanyProfile(request, userDTO);
    }

    @GetMapping("/details")
    public CompanyProfileResponse getAllCandidates(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        UserDTO userDTO = SecurityUtils.getLoggedInUser(token, jwtUtil.getKey());
        log.info("logged in user: {}", userDTO);
        return companyProfileService.getCompanyProfileDetails(userDTO);
    }

    @GetMapping("/{subdomain}")
    public CompanyProfileResponse getCompanyBySubdomain(@PathVariable String subdomain) {
        return companyProfileService.getDetailsByCompanyDomain(subdomain);
    }

    @GetMapping("/jobs")
    public List<JobPostingsResponse> getAllJobs(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        UserDTO userDTO = SecurityUtils.getLoggedInUser(token, jwtUtil.getKey());
        log.info("Get logged in user :{}", userDTO);
        return jobPostingService.getAllJobs(userDTO);

    }

    @PostMapping("/job/post")
    public JobPostingsResponse createJob(@RequestHeader("Authorization") String authHeader, @RequestBody JobPostingsRequest request) {
        log.info("The job posting request is :{}", request);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        String id = jwtUtil.extractUserObjectId(token);
        UserDTO userDTO = SecurityUtils.getLoggedInUser(token, jwtUtil.getKey());
        log.info("Logged In user: {}", userDTO);
        return companyProfileService.createJobPosting(userDTO, request);
    }


    @Data
    public static class JobPostingsRequest {
        private String title;
        private String description;
        private List<String> skillsRequired;
        private String salaryRange;
        private JobPostings.JobType jobType;
        private Integer experienceRequired;
        private String profile;
    }

    @Data
    public static class JobPostingsResponse {
        private String id;
        private String title;
        private String description;
        private List<String> skillsRequired;
        private String salaryRange;
        private JobPostings.JobType jobType;
        private Integer experienceRequired;
        private String postBy;
        private String companyId;
        private Instant createdAt;
        private boolean isActive;
        private String profile;

        public JobPostingsResponse(JobPostings job) {
            this.id = job.getId();
            this.title = job.getTitle();
            this.description = job.getDescription();
            this.skillsRequired = job.getSkillsRequired();
            this.salaryRange = job.getSalaryRange();
            this.jobType = job.getJobType();
            this.experienceRequired = job.getExperienceRequired();
            this.postBy = job.getPostBy();
            this.companyId = job.getCompanyId();
            this.createdAt = job.getCreatedAt();
            this.isActive = job.isActive();
            this.profile = job.getProfile();
        }
    }


    @Data
    public static class CompanyProfileRequest {
        private BasicSetting basicSetting;
        private ContactDetails contactDetails;
        private SocialLinks socialLinks;
    }

    @Data
    public static class CompanyProfileResponse {
        private String id;
        private String recruiterId;
        private BasicSetting basicSetting;
        private ContactDetails contactDetails;
        private SocialLinks socialLinks;
        private Instant createdAt;
        private String createdBy;
        private List<JobPostingsResponse> jobPostingsResponses;
    }
}
