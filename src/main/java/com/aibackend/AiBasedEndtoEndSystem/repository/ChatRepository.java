package com.aibackend.AiBasedEndtoEndSystem.repository;

import com.aibackend.AiBasedEndtoEndSystem.entity.Chat;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository  extends MongoRepository<Chat,String> {

    List<Chat> findByCandidateId(String candidateId);

    List<Chat> findByRecruiterId(String candidateId);

    Optional<Chat> findByRecruiterIdAndCandidateId(String recruiterId, String candidateId);

}
