package com.aibackend.AiBasedEndtoEndSystem.dto;

import java.time.Instant;
import java.util.List;

import com.aibackend.AiBasedEndtoEndSystem.entity.AiInterviewSession.InterviewSessionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiInterviewSummaryResponse {

    @JsonProperty("session_id")
    private String sessionId;

    private String backendSessionId;
    private String jobApplicationId;
    private String jobPostingId;
    private String candidateId;
    private String candidateName;
    private String resumeId;

    private InterviewSessionStatus status;
    private Double overallScore;
    private String interviewResult;
    private List<Double> detailedEvaluationScores;

    private int turnCount;
    private Instant createdAt;
    private Instant updatedAt;

    private AiInterviewJobSummaryDto job;
}
