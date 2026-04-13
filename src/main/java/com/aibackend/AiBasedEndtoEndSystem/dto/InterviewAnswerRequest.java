package com.aibackend.AiBasedEndtoEndSystem.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class InterviewAnswerRequest {

    @JsonProperty("session_id")
    @JsonAlias("sessionId")
    private String sessionId;

    private String answer;
}
