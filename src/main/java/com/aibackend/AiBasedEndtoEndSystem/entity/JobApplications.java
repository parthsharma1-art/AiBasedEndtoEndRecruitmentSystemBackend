package com.aibackend.AiBasedEndtoEndSystem.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "job_applications")
public class JobApplications {
    @Id
    private String id;
    private String candidateName;
    private String candidateEmail;
    private String mobileNumber;
    private String candidateId;
    private String jobId;
    private String recruiterId;
    private String companyId;
    private String companyName;
    private String rejectReason;
    private JobStatus status;
    private Instant appliedAt;
    private String resumeId;

    private AIShortlistStatus aiShortlistStatus;

    private String createdBy;
    private String updatedBy;
    private Instant createdAt;
    private Instant updatedAt;

    public enum JobStatus {
        APPLIED,
        UNDER_REVIEW,
        SHORTLISTED,
        REJECTED,
        TEST_SCHEDULED,
        INTERVIEW_SCHEDULED,
        HIRED
    }

    public enum AIShortlistStatus {
        SHORTLISTED,
        REJECTED,
    }
}
