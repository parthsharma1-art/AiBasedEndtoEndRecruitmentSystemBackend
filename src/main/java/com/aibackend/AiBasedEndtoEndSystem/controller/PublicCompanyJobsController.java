package com.aibackend.AiBasedEndtoEndSystem.controller;

import com.aibackend.AiBasedEndtoEndSystem.service.CompanyProfileService;
import com.aibackend.AiBasedEndtoEndSystem.service.JobPostingService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/public")
@Slf4j
public class PublicCompanyJobsController {

    @Autowired
    private JobPostingService jobPostingService;
    @Autowired
    private CompanyProfileService companyProfileService;

    @GetMapping("/jobs")
    public List<PublicJobResponse> getAllJobs() {
        log.info("Get all public jobs for the candidate");
        return jobPostingService.getAllJobs();

    }

    @GetMapping("/profiles")
    public List<CompanyProfileController.CompanyProfileResponse> getAllCompanyProfiles() {
        log.info("Get all company profiles");
        return companyProfileService.getAllCompanyProfiles();
    }

    @GetMapping("/{subdomain}")
    public CompanyProfileController.CompanyProfileResponse getCompanyBySubdomain(@PathVariable String subdomain) {
        log.info("Subdomain is :{}", subdomain);
        return companyProfileService.getDetailsByCompanyDomain(subdomain);
    }

    @Data
    public static class PublicJobResponse {
        private String id;
        private String companyId;
        private String companyName;
        private String companyDomain;
        private String recruiterId;
        private CompanyProfileController.JobPostingsResponse jobPostingsResponse;
    }
}
