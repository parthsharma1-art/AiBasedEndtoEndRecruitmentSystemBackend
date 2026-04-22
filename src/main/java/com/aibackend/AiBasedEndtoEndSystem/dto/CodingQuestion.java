package com.aibackend.AiBasedEndtoEndSystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CodingQuestion {

    private String title;
    private String description;

    @JsonProperty("sample_input")
    private String sampleInput;

    @JsonProperty("sample_output")
    private String sampleOutput;
}
