package com.aibackend.AiBasedEndtoEndSystem.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aibackend.AiBasedEndtoEndSystem.controller.CandidateResultController.CandidateResultResponseDTO;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications.AIShortlistStatus;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications.JobStatus;
import com.aibackend.AiBasedEndtoEndSystem.entity.ShortlistEvaluationResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CandidateResultService {

    @Autowired
    private JobApplicationService jobApplicationService;
    @Autowired
    private ShortlistEvaluationResultService shortlistEvaluationResultService;
    
    public List<CandidateResultResponseDTO> getCandidateResult(UserDTO userDTO) {
        log.info("Getting candidate result for user: {}", userDTO.getId());

        List<JobApplications> jobApplications = jobApplicationService.getAllJobApplicationsDetailsByCandidateId(userDTO.getId());
        if (jobApplications.isEmpty()) {
            return Collections.emptyList();
        }
        return jobApplications.stream().map(this::toCandidateResultResponseDTO).collect(Collectors.toList());
    }

    private CandidateResultResponseDTO toCandidateResultResponseDTO(JobApplications jobApplication) {
        ShortlistEvaluationResult eval = shortlistEvaluationResultService
                .getShortlistEvaluationForJobApplication(jobApplication.getId());

        CandidateResultResponseDTO dto = new CandidateResultResponseDTO();
        dto.setJobId(jobApplication.getJobId());
        dto.setCandidateId(jobApplication.getCandidateId());
        dto.setRejectedReason(jobApplication.getRejectReason());
        dto.setInsights(CandidateResultResponseDTO.Insights.builder()
                .skillsMatchPercentage(toPercentInt(eval != null ? eval.getSkillsMatchRatio() : null))
                .similarityPercentage(toPercentInt(eval != null ? eval.getSimilarity() : null))
                .build());

        dto.setSkills(CandidateResultResponseDTO.Skills.builder()
                .matched(eval != null ? eval.getMatchedSkills() : null)
                .missing(eval != null ? eval.getMissingSkills() : null)
                .build());

        dto.setApplication(CandidateResultResponseDTO.Application.builder()
                .status(jobApplication.getStatus())
                .appliedAt(jobApplication.getAppliedAt())
                .build());
        return dto;
    }

    /** Ratios are stored as 0–1; API exposes whole-number percentages. */
    private static Integer toPercentInt(Double ratio) {
        if (ratio == null) {
            return null;
        }
        return (int) Math.round(ratio * 100.0);
    }

    /**
     * Prefer AI shortlist outcome when present; otherwise fall back to workflow status.
     */
    private static JobStatus resolveResultJobStatus(JobApplications jobApplication) {
        AIShortlistStatus ai = jobApplication.getAiShortlistStatus();
        if (ai == AIShortlistStatus.SHORTLISTED) {
            return JobStatus.SHORTLISTED;
        }
        if (ai == AIShortlistStatus.REJECTED) {
            return JobStatus.REJECTED;
        }
        return jobApplication.getStatus();
    }

}
