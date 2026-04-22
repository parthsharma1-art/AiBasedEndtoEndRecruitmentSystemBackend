package com.aibackend.AiBasedEndtoEndSystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class McqQuestion {

    private String question;
    private List<String> options;

    @JsonProperty("correct_answer")
    private String correctAnswer;
}
