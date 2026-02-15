package com.aibackend.AiBasedEndtoEndSystem.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.CompanyProfile.BasicSetting;
import com.aibackend.AiBasedEndtoEndSystem.entity.CompanyProfile.ContactDetails;
import com.aibackend.AiBasedEndtoEndSystem.entity.CompanyProfile.SocialLinks;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobPostings;
import com.aibackend.AiBasedEndtoEndSystem.service.CompanyProfileService;
import com.aibackend.AiBasedEndtoEndSystem.service.JobPostingService;
import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/profile")
public class CompanyProfileController {

    @Autowired
    private CompanyProfileService companyProfileService;
    @Autowired
    private JobPostingService jobPostingService;

    @PutMapping("/update")
    public CompanyProfileResponse updateCompanyProfile(@RequestBody CompanyProfileRequest request) {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        return companyProfileService.updateCompanyProfile(request, user);
    }

    @GetMapping("/details")
    public CompanyProfileResponse getAllCandidates() {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        log.info("logged in user: {}", user);
        return companyProfileService.getCompanyProfileDetails(user);
    }

    @GetMapping("/{subdomain}")
    public CompanyProfileResponse getCompanyBySubdomain(@PathVariable String subdomain) {
        return companyProfileService.getDetailsByCompanyDomain(subdomain);
    }

    @GetMapping("/jobs")
    public List<JobPostingsResponse> getAllJobs() {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        log.info("Get logged in user :{}", user);
        return jobPostingService.getAllJobs(user);

    }

    @PostMapping("/job/post")
    public JobPostingsResponse createJob(@RequestBody JobPostingsRequest request) {
        log.info("The job posting request is :{}", request);
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        log.info("Logged In user: {}", user);
        return companyProfileService.createJobPosting(user, request);
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
