package com.aibackend.AiBasedEndtoEndSystem.service;

import com.aibackend.AiBasedEndtoEndSystem.controller.CompanyProfileController;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.CompanyProfile;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobPostings;
import com.aibackend.AiBasedEndtoEndSystem.repository.JobPostingRepository;
import com.aibackend.AiBasedEndtoEndSystem.util.UniqueUtiliy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class JobPostingService {
    @Autowired
    private UniqueUtiliy uniqueUtiliy;
    @Autowired
    private JobPostingRepository repository;
    @Autowired
    @Lazy
    private CompanyProfileService companyProfileService;

    public JobPostings createJob(CompanyProfileController.JobPostingsRequest request, CompanyProfile companyProfile) {
        JobPostings jobPostings = new JobPostings();
        jobPostings.setId(uniqueUtiliy.getNextNumber("JOB_POSTING", "jobPosting"));
        jobPostings.setCompanyId(companyProfile.getId());
        jobPostings.setActive(Boolean.TRUE);
        jobPostings.setPostBy(companyProfile.getRecruiterId());
        jobPostings.setJobType(request.getJobType());
        jobPostings.setCreatedAt(Instant.now());
        jobPostings.setCreatedBy(companyProfile.getRecruiterId());
        jobPostings.setDescription(request.getDescription());
        jobPostings.setTitle(request.getTitle());
        jobPostings.setUpdatedAt(Instant.now());
        jobPostings.setSkillsRequired(request.getSkillsRequired());
        jobPostings.setExperienceRequired(request.getExperienceRequired());
        jobPostings.setUpdatedBy(companyProfile.getRecruiterId());
        jobPostings.setSalaryRange(request.getSalaryRange());
        return save(jobPostings);
    }

    public JobPostings save(JobPostings jobPostings) {
        log.info("Saving new job for the id :{}", jobPostings.getId());
        return repository.save(jobPostings);

    }

    public List<CompanyProfileController.JobPostingsResponse> getAllJobs(UserDTO user) {
        log.info("Getting jobs for the user :{}", user);
        CompanyProfile companyProfile = companyProfileService.getCompanyProfileByRecruiterId(user.getId());
        if (ObjectUtils.isEmpty(companyProfile)) {
            return null;
        }
        List<JobPostings> jobPostings = repository.findByCompanyId(companyProfile.getId());
        List<CompanyProfileController.JobPostingsResponse> responses = new ArrayList<>();
        for (JobPostings postings : jobPostings) {
            responses.add(new CompanyProfileController.JobPostingsResponse(postings));
        }
        return responses;

    }
}
