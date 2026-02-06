package com.aibackend.AiBasedEndtoEndSystem.controller;

import com.aibackend.AiBasedEndtoEndSystem.service.JobPostingService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/public")
@Slf4j
public class PublicCompanyJobsController {

    @Autowired
    private JobPostingService jobPostingService;

    @GetMapping("/jobs")
    public List<PublicJobResponse> getAllJobs() {
        log.info("Get all public jobs for the candidate");
        return jobPostingService.getAllJobs();

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
