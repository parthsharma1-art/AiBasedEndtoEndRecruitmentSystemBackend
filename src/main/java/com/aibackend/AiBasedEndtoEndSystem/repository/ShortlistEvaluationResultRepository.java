package com.aibackend.AiBasedEndtoEndSystem.repository;

import com.aibackend.AiBasedEndtoEndSystem.entity.ShortlistEvaluationResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShortlistEvaluationResultRepository extends MongoRepository<ShortlistEvaluationResult, String> {

    List<ShortlistEvaluationResult> findByJobPostingIdOrderByEvaluatedAtDesc(String jobPostingId);

    List<ShortlistEvaluationResult> findByCandidateIdOrderByEvaluatedAtDesc(String candidateId);

    Optional<ShortlistEvaluationResult> findFirstByJobPostingIdAndCandidateIdOrderByEvaluatedAtDesc(
            String jobPostingId,
            String candidateId);

    Optional<ShortlistEvaluationResult> findFirstByJobApplicationIdOrderByEvaluatedAtDesc(String jobApplicationId);
}
