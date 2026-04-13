package com.aibackend.AiBasedEndtoEndSystem.dto;

import java.util.List;

import com.aibackend.AiBasedEndtoEndSystem.entity.JobPostings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInterviewJobSummaryDto {
    private String jobId;
    private String title;
    private String companyName;
    private JobPostings.JobType jobType;
    private Integer experienceRequired;
    private String profile;
    private List<String> skillsRequired;
}
