package com.aibackend.AiBasedEndtoEndSystem.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.aibackend.AiBasedEndtoEndSystem.entity.CandidateResumeAtsEvaluation;

@Repository
public interface CandidateResumeAtsEvaluationRepository extends MongoRepository<CandidateResumeAtsEvaluation, String> {
    List<CandidateResumeAtsEvaluation> findByCandidateIdOrderByEvaluatedAtDesc(String candidateId);
}
