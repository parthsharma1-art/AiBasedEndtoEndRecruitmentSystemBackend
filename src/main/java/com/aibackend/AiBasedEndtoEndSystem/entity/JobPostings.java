package com.aibackend.AiBasedEndtoEndSystem.entity;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "job_postings")
public class JobPostings {
    @Id
    private ObjectId id;
    private String title;
    private String description;
    private List<String> skillsRequired;
    private String salaryRange;
    private JobType jobType;
    private Integer experienceRequired;
    private String postBy;   // recruiter Id
    private Instant createdAt;
    private Instant createdBy;
    private Instant updatedAt;
    private Instant updatedBy;
    private boolean isActive;


    public enum JobType {
        REMOTE, HYBRID, ONSITE
    }
}
