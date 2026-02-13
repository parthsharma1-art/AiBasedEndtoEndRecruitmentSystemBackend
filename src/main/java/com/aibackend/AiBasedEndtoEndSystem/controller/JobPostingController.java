package com.aibackend.AiBasedEndtoEndSystem.controller;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.service.CompanyProfileService;
import com.aibackend.AiBasedEndtoEndSystem.service.JobPostingService;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile/job")
@Slf4j
public class JobPostingController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JobPostingService jobPostingService;
    @Autowired
    private CompanyProfileService companyProfileService;


    @DeleteMapping("delete/{id}")
    public Boolean deleteById(@PathVariable String id, @RequestHeader("Authorization") String authHeader) {
        log.info("Delete job by ID:{}", id);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        UserDTO userDTO = SecurityUtils.getLoggedInUser(token, jwtUtil.getKey());
        return jobPostingService.deleteJobById(id, userDTO);
    }


    @PutMapping("update/{jobId}")
    public CompanyProfileController.JobPostingsResponse updateJob(@RequestHeader("Authorization") String authHeader, @PathVariable String jobId, @RequestBody CompanyProfileController.JobPostingsRequest request) {
        log.info("The Update Job for the Id :{}", jobId);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        String id = jwtUtil.extractUserObjectId(token);
        UserDTO userDTO = SecurityUtils.getLoggedInUser(token, jwtUtil.getKey());
        return companyProfileService.updateJobRequest(userDTO, jobId, request);

    }

    @GetMapping("get/{jobId}")
    public CompanyProfileController.JobPostingsResponse getJobById(@RequestHeader("Authorization") String authHeader, @PathVariable String jobId) {
        log.info("Getting Job for the Id :{}", jobId);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        UserDTO userDTO = SecurityUtils.getLoggedInUser(token, jwtUtil.getKey());
        return jobPostingService.getJobDetailsById(userDTO, jobId);

    }

}

