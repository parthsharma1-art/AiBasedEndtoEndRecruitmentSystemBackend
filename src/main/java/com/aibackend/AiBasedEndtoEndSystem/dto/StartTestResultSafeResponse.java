package com.aibackend.AiBasedEndtoEndSystem.dto;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartTestResultSafeResponse {

    private String id;
    private String jobApplicationId;
    private String jobId;
    private String candidateId;
    private Instant createdAt;
    private List<McqQuestionSafeResponse> mcqs;
    private List<CodingQuestionSafeResponse> codingQuestions;
}
