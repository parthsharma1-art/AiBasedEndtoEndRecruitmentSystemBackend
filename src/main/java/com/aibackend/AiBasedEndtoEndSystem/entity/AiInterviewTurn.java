package com.aibackend.AiBasedEndtoEndSystem.entity;

import java.time.Instant;

import lombok.Data;

@Data
public class AiInterviewTurn {
    private int turnIndex;
    private String aiQuestion;
    private Instant questionRecordedAt;
    private String candidateAnswer;
    private Instant candidateAnsweredAt;
    /** Serialized JSON from {@code POST /interview/answer} after this turn's answer. */
    private String postAnswerAiResponseJson;
}
