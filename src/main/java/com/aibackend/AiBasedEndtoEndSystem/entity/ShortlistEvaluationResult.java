package com.aibackend.AiBasedEndtoEndSystem.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
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
    @Indexed
    private String jobApplicationId;
    private String resumeId;

    private Instant evaluatedAt;

    private Instant createdAt;
    private Instant updatedAt;
}
