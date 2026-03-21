package com.aibackend.AiBasedEndtoEndSystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Shape of JSON returned by FastAPI {@code POST /test/generate}. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiGeneratedTestPayload {

    private List<McqQuestion> mcqs = new ArrayList<>();

    @JsonProperty("coding_questions")
    private List<CodingQuestion> codingQuestions = new ArrayList<>();
}
