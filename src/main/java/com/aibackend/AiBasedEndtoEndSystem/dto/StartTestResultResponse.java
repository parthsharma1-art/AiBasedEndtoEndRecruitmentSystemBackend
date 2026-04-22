package com.aibackend.AiBasedEndtoEndSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartTestResultResponse {

    private String id;
    private String jobApplicationId;
    private String jobId;
    private String candidateId;
    private Instant createdAt;
    private List<McqQuestion> mcqs;
    private List<CodingQuestion> codingQuestions;
}
