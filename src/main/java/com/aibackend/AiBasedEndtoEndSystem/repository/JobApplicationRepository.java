package com.aibackend.AiBasedEndtoEndSystem.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications;

@Repository
public interface JobApplicationRepository extends MongoRepository<JobApplications, String> {

    List<JobApplications> findByCandidateId(String candidateId);

    Optional<JobApplications> findByRecruiterId(String recruiterId);

    Optional<JobApplications> findByCompanyId(String companyId);

    Optional<JobApplications> findByCandidateIdAndJobId(String candidateId,String jobId);

    List<JobApplications> findByJobId(String jobId);

    List<JobApplications> findByJobIdAndStatus(String jobId, JobApplications.JobStatus status);

    List<JobApplications> findByStatusAndCreatedAtBefore(JobApplications.JobStatus status,Instant createdAt);

}
