package com.aibackend.AiBasedEndtoEndSystem.entity;

import com.aibackend.AiBasedEndtoEndSystem.dto.CodingQuestion;
import com.aibackend.AiBasedEndtoEndSystem.dto.McqQuestion;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "job_application_generated_tests")
public class JobApplicationGeneratedTest {
    @Id
    private String id;
    @Indexed(unique = true)
    private String jobApplicationId;
    private String candidateId;
    private String jobId;
    private Instant createdAt;
    private List<McqQuestion> mcqs;
    private List<CodingQuestion> codingQuestions;


    
    private List<String> submittedMcqAnswers;
    private List<String> submittedCodingAnswers;
    private List<Boolean> mcqEvaluations;
    private List<Boolean> codingEvaluations;
    private Instant evaluatedAt;
}
