package com.aibackend.AiBasedEndtoEndSystem.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "ai_interviews")
public class AiInterviewSession {
    @Id
    private String id;
    @Indexed
    private String pythonSessionId;
    @Indexed
    private String jobApplicationId;
    private String jobId;
    @Indexed
    private String candidateId;
    private String resumeId;
    private InterviewSessionStatus status;
    private List<AiInterviewTurn> turns = new ArrayList<>();
    private Double overallScore;
    private String interviewResult;
    private List<Double> detailedEvaluationScores;
    private String startResponseJson;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;

    public enum InterviewSessionStatus {
        IN_PROGRESS,
        COMPLETED
    }
}
