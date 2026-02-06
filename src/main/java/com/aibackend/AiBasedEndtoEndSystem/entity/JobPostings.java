package com.aibackend.AiBasedEndtoEndSystem.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "job_postings")
public class JobPostings {
    @Id
    private String id;
    private String title;
    private String description;
    private List<String> skillsRequired;
    private String salaryRange;
    private JobType jobType;
    private Integer experienceRequired;
    private String postBy;   // recruiter Id
    private String companyId;
    private Instant createdAt;
    private String createdBy;
    private Instant updatedAt;
    private String updatedBy;
    private boolean isActive;


    public enum JobType {
        REMOTE, HYBRID, ONSITE
    }
}
