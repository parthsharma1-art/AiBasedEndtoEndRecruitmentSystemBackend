package com.aibackend.AiBasedEndtoEndSystem.controller;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.service.CompanyProfileService;
import com.aibackend.AiBasedEndtoEndSystem.service.JobPostingService;
import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/profile/job")
@Slf4j
public class JobPostingController {
    @Autowired
    private JobPostingService jobPostingService;
    @Autowired
    private CompanyProfileService companyProfileService;

    @DeleteMapping("delete/{id}")
    public Boolean deleteById(@PathVariable String id) {
        log.info("Delete job by ID:{}", id);
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        return jobPostingService.deleteJobById(id, user);
    }

    @PutMapping("update/{jobId}")
    public CompanyProfileController.JobPostingsResponse updateJob(@PathVariable String jobId,
            @RequestBody CompanyProfileController.JobPostingsRequest request) {
        log.info("The Update Job for the Id :{}", jobId);
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        return companyProfileService.updateJobRequest(user, jobId, request);

    }

    @GetMapping("get/{jobId}")
    public CompanyProfileController.JobPostingsResponse getJobById(@PathVariable String jobId) {
        log.info("Getting Job for the Id :{}", jobId);
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        return jobPostingService.getJobDetailsById(user, jobId);

    }

}
