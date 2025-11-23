package com.aibackend.AiBasedEndtoEndSystem.repository;

import com.aibackend.AiBasedEndtoEndSystem.entity.Candidate;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateRepository extends MongoRepository<Candidate, ObjectId> {

    Optional<Candidate> findByMobileNumber(String mobileNumber);
}
