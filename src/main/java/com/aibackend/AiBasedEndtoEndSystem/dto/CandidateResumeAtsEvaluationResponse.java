package com.aibackend.AiBasedEndtoEndSystem.dto;

import java.time.Instant;
import java.util.List;

import lombok.Data;

@Data
public class CandidateResumeAtsEvaluationResponse {
    private String id;
    private String candidateId;
    private String resumeId;
    private Double atsScore;
    private Feedback feedback;
    private Instant evaluatedAt;

    @Data
    public static class Feedback {
        private String level;
        private List<String> strengths;
        private List<String> improvementAreas;
    }
}
