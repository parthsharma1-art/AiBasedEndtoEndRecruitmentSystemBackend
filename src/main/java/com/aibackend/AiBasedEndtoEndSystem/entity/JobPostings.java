package com.aibackend.AiBasedEndtoEndSystem.entity;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "job_postings")
public class JobPostings {
    @Id
    private String id;
    private String title;
    private String description;
    private List<String> skillsRequired;
    @Deprecated
    private String salaryRange;
    private JobType jobType;
    private Integer experienceRequired;
    private String profile;
    private List<String> locations;
    private String postBy;
    private boolean isAssessmentRequired = true;
    private boolean isInterviewRequired = false;
    private SalaryRangeLpa salaryRangeInLPA;
    private Double shortlistPercentage;
    private String currency;
    private String companyId;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    private boolean isActive;

    public enum JobType {
        REMOTE, HYBRID, ONSITE, FULL_TIME, INTERNSHIP
    }
}
