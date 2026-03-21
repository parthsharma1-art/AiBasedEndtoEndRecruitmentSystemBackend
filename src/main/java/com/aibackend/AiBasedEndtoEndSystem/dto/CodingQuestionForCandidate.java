package com.aibackend.AiBasedEndtoEndSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Coding prompt for the candidate (no sample / expected output). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodingQuestionForCandidate {

    private String title;
    private String description;
    private String sampleInput;
}
