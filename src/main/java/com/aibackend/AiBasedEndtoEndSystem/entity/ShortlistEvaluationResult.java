package com.aibackend.AiBasedEndtoEndSystem.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "shortlist_evaluation_results")
public class ShortlistEvaluationResult {

    @Id
    private String id;

    private Boolean shortlisted;
    private Double score;
    private Double similarity;
    private Double skillsMatchRatio;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private String candidateName;

    private String candidateId;
    private String jobPostingId;
    /** Job application row id when evaluation ran from an application batch. */
    private String jobApplicationId;
    /** GridFS file id of the resume used for this evaluation. */
    private String resumeId;

    private Instant evaluatedAt;

    private Instant createdAt;
    private Instant updatedAt;
}
