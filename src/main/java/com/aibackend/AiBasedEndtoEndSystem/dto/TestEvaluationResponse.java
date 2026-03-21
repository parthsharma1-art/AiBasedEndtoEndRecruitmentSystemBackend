package com.aibackend.AiBasedEndtoEndSystem.dto;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestEvaluationResponse {

    private String jobApplicationId;
    private int mcqTotal;
    private int mcqCorrectCount;
    private int codingTotal;
    private int codingCorrectCount;
    private List<Boolean> mcqPerQuestionCorrect;
    private List<Boolean> codingPerQuestionCorrect;
    private Instant evaluatedAt;
}
