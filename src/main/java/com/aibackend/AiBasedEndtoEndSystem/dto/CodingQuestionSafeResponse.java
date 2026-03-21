package com.aibackend.AiBasedEndtoEndSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodingQuestionSafeResponse {

    private String title;
    private String description;
    private String sampleOutput;
}
