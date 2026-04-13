package com.aibackend.AiBasedEndtoEndSystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.aibackend.AiBasedEndtoEndSystem.entity.AiInterviewSession;

public interface AiInterviewSessionRepository extends MongoRepository<AiInterviewSession, String> {

    Optional<AiInterviewSession> findByPythonSessionIdAndJobApplicationId(String pythonSessionId,
            String jobApplicationId);

    Optional<AiInterviewSession> findFirstByJobApplicationIdOrderByUpdatedAtDesc(String jobApplicationId);

    List<AiInterviewSession> findByCandidateIdOrderByUpdatedAtDesc(String candidateId);
}
