package com.aibackend.AiBasedEndtoEndSystem.repository;

import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplicationGeneratedTest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobApplicationGeneratedTestRepository extends MongoRepository<JobApplicationGeneratedTest, String> {

    Optional<JobApplicationGeneratedTest> findByJobApplicationId(String jobApplicationId);
}
