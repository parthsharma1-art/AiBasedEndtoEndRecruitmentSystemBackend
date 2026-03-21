package com.aibackend.AiBasedEndtoEndSystem.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class TestAnswerSubmissionRequest {

    private List<String> mcqAnswers = new ArrayList<>();

    private List<String> codingAnswers = new ArrayList<>();
}
