package com.aibackend.AiBasedEndtoEndSystem.service;

import com.aibackend.AiBasedEndtoEndSystem.controller.CompanyProfileController;
import com.aibackend.AiBasedEndtoEndSystem.controller.JobPostingController;
import com.aibackend.AiBasedEndtoEndSystem.controller.PublicCompanyJobsController.PublicJobResponse;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.CompanyProfile;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobApplications;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobPostings;
import com.aibackend.AiBasedEndtoEndSystem.entity.Recruiter;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.repository.JobPostingRepository;
import com.aibackend.AiBasedEndtoEndSystem.util.UniqueUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobPostingService {

    private final UniqueUtility uniqueUtility;
    private final JobPostingRepository repository;
    @Lazy
    private final RecruiterService recruiterService;
    private final JobApplicationService jobApplicationService;

    @Lazy
    private final CompanyProfileService companyProfileService;

    public JobPostings createJob(
            CompanyProfileController.JobPostingsRequest request,
            CompanyProfile companyProfile) {
        log.info("Creating new job for company: {}", companyProfile.getId());
        JobPostings job = new JobPostings();
        job.setId(uniqueUtility.getNextNumber("JOB_POSTING", "jobPosting"));
        job.setCompanyId(companyProfile.getId());
        job.setPostBy(companyProfile.getRecruiterId());
        job.setActive(Boolean.TRUE);
        Instant now = Instant.now();
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        job.setCreatedBy(companyProfile.getRecruiterId());
        job.setUpdatedBy(companyProfile.getRecruiterId());
        applyRequestToJob(job, request);
        return save(job);
    }

    public JobPostings save(JobPostings job) {
        log.info("Saving job posting id: {}", job.getId());
        return repository.save(job);
    }

    private void applyRequestToJob(JobPostings job, CompanyProfileController.JobPostingsRequest request) {
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setJobType(request.getJobType());
        job.setProfile(request.getProfile());
        job.setSkillsRequired(request.getSkillsRequired());
        job.setExperienceRequired(request.getExperienceRequired());
        job.setSalaryRange(request.getSalaryRange());
    }

    public List<JobPostings> getAllJobPostings(Recruiter recruiter) {
        CompanyProfile profile = companyProfileService.getCompanyProfileByRecruiterId(recruiter.getId());
        if (ObjectUtils.isEmpty(profile)) {
            return Collections.emptyList();
        }
        return repository.findByCompanyId(profile.getId());
    }

    public List<CompanyProfileController.JobPostingsResponse> getAllJobs(UserDTO user) {
        log.info("Fetching jobs for user: {}", user.getId());
        CompanyProfile profile = companyProfileService.getCompanyProfileByRecruiterId(user.getId());
        if (ObjectUtils.isEmpty(profile)) {
            return Collections.emptyList();
        }
        if (!profile.getRecruiterId().equals(user.getId())) {
            log.error("Unauthorize access to the Company profile :{}", profile.getId());
            throw new BadException("Unauthorize access to the Company profile " + user.getId());
        }
        return repository.findByCompanyId(profile.getId())
                .stream()
                .peek(job -> log.info("Job profile value: {}", job.getProfile()))
                .map(CompanyProfileController.JobPostingsResponse::new)
                .collect(Collectors.toList());

    }

    public List<PublicJobResponse> getAllJobs() {
        log.info("Fetching all public active jobs");
        List<JobPostings> jobs = repository.findByIsActiveTrue(Boolean.TRUE);
        if (jobs.isEmpty()) {
            return Collections.emptyList();
        }
        log.info("jobs list found :{}", jobs);
        // Collect company IDs
        Set<String> companyIds = jobs.stream()
                .map(JobPostings::getCompanyId)
                .collect(Collectors.toSet());
        log.info("list of company ids :{}", companyIds);
        Map<String, CompanyProfile> companyMap = companyProfileService.getCompanyProfilesByIds(companyIds);
        log.info("company map is here :{}", companyMap);
        List<PublicJobResponse> responses = new ArrayList<>();
        for (JobPostings job : jobs) {
            CompanyProfile profile = companyMap.get(job.getCompanyId());
            if (profile == null || ObjectUtils.isEmpty(profile.getBasicSetting())) {
                log.warn("Company not found for id: {}", job.getCompanyId());
                continue;
            }

            PublicJobResponse response = getJobResponse(job, profile);
            response.setTotalAppliedCandidates(jobApplicationService.totalJobAppliedCandidate(job.getId()));
            responses.add(response);
        }

        return responses;
    }

    private PublicJobResponse getJobResponse(JobPostings job, CompanyProfile profile) {
        PublicJobResponse response = new PublicJobResponse();
        response.setId(job.getId());
        response.setRecruiterId(profile.getRecruiterId());
        response.setCompanyId(profile.getId());
        response.setCompanyName(
                profile.getBasicSetting().getCompanyName());
        response.setCompanyDomain(
                profile.getBasicSetting().getCompanyDomain());
        response.setJobPostingsResponse(
                new CompanyProfileController.JobPostingsResponse(job));
        return response;
    }

    public List<CompanyProfileController.JobPostingsResponse> allJobsByCompanyId(String companyId) {

        log.info("Fetching jobs for company: {}", companyId);

        return repository.findByCompanyId(companyId)
                .stream()
                .map(CompanyProfileController.JobPostingsResponse::new)
                .collect(Collectors.toList());
    }


    public JobPostings updateJobPosting(
            String id,
            CompanyProfile companyProfile,
            Recruiter recruiter,
            CompanyProfileController.JobPostingsRequest request) {

        log.info("Updating job id: {}", id);

        JobPostings job = repository.findById(id)
                .orElseThrow(() -> new BadException("Job Posting not found: " + id));

        applyRequestToJob(job, request);

        job.setCompanyId(companyProfile.getId());
        job.setPostBy(companyProfile.getRecruiterId());
        job.setActive(Boolean.TRUE);
        job.setUpdatedAt(Instant.now());
        job.setUpdatedBy(recruiter.getId());

        return save(job);
    }

    public Boolean deleteJobById(String id, UserDTO user) {
        log.info("Deleting job by id :{}", id);
        JobPostings jobPostings = repository.findById(id).orElse(null);
        if (ObjectUtils.isEmpty(jobPostings)) {
            log.error("job not found for the id :{}", id);
            throw new BadException("Job not found " + id);
        }
        repository.delete(jobPostings);
        return true;
    }

    public CompanyProfileController.JobPostingsResponse getJobDetailsById(UserDTO user, String id) {
        log.info("Get job details for the id :{}", id);
        JobPostings jobPostings = repository.findById(id).orElse(null);
        if (ObjectUtils.isEmpty(jobPostings)) {
            log.info("Job post not found");
            return null;
        }
        return new CompanyProfileController.JobPostingsResponse(jobPostings);

    }

    public JobPostings getJobPostingById(String id) {
        log.info("Get job Posting for the id :{}", id);
        JobPostings jobPostings = repository.findById(id).orElse(null);
        if (ObjectUtils.isEmpty(jobPostings)) {
            log.info("Job not found for the id " + id);
            return null;
        }
        return jobPostings;

    }

    public List<JobPostingController.JobApplicationResponse> getAllJobApplications(UserDTO user, String jobId) {
        log.info("Get all job applications for the user :{} and jobID :{}", user, jobId);
        Recruiter recruiter = recruiterService.getRecruiterById(user.getId());
        if (ObjectUtils.isEmpty(recruiter)) {
            throw new BadException("Recruiter not found for the ID " + user.getId());
        }
        JobPostings jobPostings = getJobPostingById(jobId);
        if (ObjectUtils.isEmpty(jobPostings)) {
            throw new BadException("Job not found for the ID " + jobId);
        }
        return jobApplicationService.getAllJobApplications(jobPostings);

    }
}
