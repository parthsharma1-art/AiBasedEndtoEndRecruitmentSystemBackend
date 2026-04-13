package com.aibackend.AiBasedEndtoEndSystem.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for {@code POST .../interview/start} and {@code POST .../interview/answer}: question to
 * show, session id for the next answer call, and basic job/candidate context for the UI.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiInterviewApiResponse {
    @JsonProperty("session_id")
    private String sessionId;
    private String backendSessionId;

    private String jobApplicationId;
    private String jobPostingId;
    private String candidateId;
    private String candidateName;
    private String resumeId;

    private String question;
    private boolean interviewComplete;
    private String closingMessage;
    private int turnCount;
    private Double overallScore;
    private String interviewResult;
    private List<Double> detailedEvaluationScores;

    private AiInterviewJobSummaryDto job;
}
