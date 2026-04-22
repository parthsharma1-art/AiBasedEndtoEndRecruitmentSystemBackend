package com.aibackend.AiBasedEndtoEndSystem.dto;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationGeneratedTestDto {
    private String id;
    private String jobApplicationId;
    private String candidateId;
    private String jobId;
    private Instant createdAt;
    private List<McqQuestion> mcqs;
    private List<CodingQuestion> codingQuestions;
    private List<String> submittedMcqAnswers;
    private List<String> submittedCodingAnswers;
    private List<Boolean> mcqEvaluations;
    private List<Boolean> codingEvaluations;
    private Instant evaluatedAt;
}
