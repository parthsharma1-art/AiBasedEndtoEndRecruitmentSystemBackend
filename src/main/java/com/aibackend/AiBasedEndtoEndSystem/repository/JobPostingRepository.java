package com.aibackend.AiBasedEndtoEndSystem.repository;

import com.aibackend.AiBasedEndtoEndSystem.entity.JobPostings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import static java.lang.Boolean.TRUE;

@Repository
public interface JobPostingRepository extends MongoRepository<JobPostings, String> {

    List<JobPostings> findByCompanyId(String companyId);

    List<JobPostings> findByIsActiveTrue(Boolean isActive);
}
