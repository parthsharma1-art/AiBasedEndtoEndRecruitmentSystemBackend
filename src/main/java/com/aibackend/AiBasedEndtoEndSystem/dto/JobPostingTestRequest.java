package com.aibackend.AiBasedEndtoEndSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON body for FastAPI {@code POST /test/generate} ({@code JobPostingTestRequest}).
 * Extra Mongo-style fields can be added later; FastAPI uses {@code extra="ignore"}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobPostingTestRequest {

    private String title;
    private String description = "";
    private List<String> skillsRequired = new ArrayList<>();
    private String difficulty = "Intermediate";
    private Double experienceRequired;
    private String profile;
    private String jobType;
    private String salaryRange;
}
