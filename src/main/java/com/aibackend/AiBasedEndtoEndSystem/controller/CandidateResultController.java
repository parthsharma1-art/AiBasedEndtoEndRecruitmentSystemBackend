package com.aibackend.AiBasedEndtoEndSystem.controller;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications;
import com.aibackend.AiBasedEndtoEndSystem.service.CandidateResultService;
import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;

import lombok.Builder;
import lombok.Data;

@RestController
@RequestMapping("/candidate-result")
public class CandidateResultController {
    @Autowired
    private CandidateResultService candidateResultService;

    @GetMapping("")
    public List<CandidateResultResponseDTO> getCandidateResult() {
        UserDTO userDTO = SecurityUtils.getLoggedInUser();
        if (userDTO == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        }
        return candidateResultService.getCandidateResult(userDTO);
    }

    @Data
    public static class CandidateResultResponseDTO {

        private String jobId;
        private String candidateId;

        private Insights insights;
        private Skills skills;
        private Application application;
        private String rejectedReason;

        @Data
        @Builder
        public static class Insights {
            private Integer skillsMatchPercentage;
            private Integer similarityPercentage;
        }

        @Data
        @Builder
        public static class Skills {
            private List<String> matched;
            private List<String> missing;
        }

        @Data
        @Builder
        public static class Application {
            private JobApplications.JobStatus status;
            private Instant appliedAt;
        }
    }

}
