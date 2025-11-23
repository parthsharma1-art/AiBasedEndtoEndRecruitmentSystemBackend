package com.aibackend.AiBasedEndtoEndSystem.entity;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "applications")
public class Application {

    @Id
    private ObjectId id;

    private String candidateId;
    private String jobId;

    private ApplicationStatus status;

    private Double aiMatchScore;        // score specific to job, not overall resume score
    private LocalDateTime appliedAt = LocalDateTime.now();

    public enum ApplicationStatus {
        SUBMITTED,
        SHORTLISTED,
        INTERVIEW_SCHEDULED,
        REJECTED,
        SELECTED
    }
}

