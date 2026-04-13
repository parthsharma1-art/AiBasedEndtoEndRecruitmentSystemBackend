package com.aibackend.AiBasedEndtoEndSystem.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiInterviewTurnOutDto {
    private int turnIndex;
    private String aiQuestion;
    private Instant questionRecordedAt;
    private String candidateAnswer;
    private Instant candidateAnsweredAt;
    private Double score;
    private String feedback;
    private String nextQuestionPreview;
}
