package com.aibackend.AiBasedEndtoEndSystem.entity;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "candidate_resume_ats_evaluations")
public class CandidateResumeAtsEvaluation {
    @Id
    private String id;
    private String candidateId;
    private String resumeId;
    private Double atsScore;
    private Feedback feedback;
    private Instant evaluatedAt;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    public static class Feedback {
        private String level;
        private List<String> strengths;
        private List<String> improvementAreas;
    }
}
