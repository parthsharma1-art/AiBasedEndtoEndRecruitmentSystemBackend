package com.aibackend.AiBasedEndtoEndSystem.service;


import com.aibackend.AiBasedEndtoEndSystem.controller.CandidateApplyJobController;
import com.aibackend.AiBasedEndtoEndSystem.controller.JobPostingController;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.*;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.repository.JobApplicationRepository;
import com.aibackend.AiBasedEndtoEndSystem.util.UniqueUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class JobApplicationService {
    @Autowired
    private JobApplicationRepository repository;
    @Autowired
    private CompanyProfileService companyProfileService;
    @Autowired
    private CandidateService candidateService;
    @Autowired
    private RecruiterService recruiterService;
    @Autowired
    @Lazy
    private JobPostingService jobPostingService;
    @Autowired
    private UniqueUtility uniqueUtility;
    @Autowired
    private FileStorageService fileStorageService;

    public Boolean createNewJobApplications(UserDTO user, CandidateApplyJobController.ApplyJobRequest request, String jobId, MultipartFile resume) {
        log.info("Creating new Job Application for the user :{}", user);
        if (ObjectUtils.isEmpty(request.getUseSameResume())) {
            throw new BadException("Resume value is required");
        }
        if (!request.getUseSameResume() && ObjectUtils.isEmpty(resume)) {
            throw new BadException("Resume is required");
        }
        if (ObjectUtils.isEmpty(request.getMobileNumber())) {
            throw new BadException("Mobile Number is required");
        }
        if (!request.getUseSameEmail() && ObjectUtils.isEmpty(request.getEmail())) {
            throw new BadException("Email is required");
        }
        Candidate candidate = candidateService.getCandidateById(user.getId());
        if (ObjectUtils.isEmpty(candidate)) {
            throw new BadException("Candidate not found for the ID :" + user.getId());
        }
        JobPostings jobPostings = jobPostingService.getJobPostingById(jobId);
        if (ObjectUtils.isEmpty(jobPostings)) {
            throw new BadException("Job not found for the id " + jobId);
        }
        Optional<JobApplications> jobApplications = repository.findByCandidateIdAndJobId(candidate.getId(), jobPostings.getId());
        if (jobApplications.isPresent()) {
            throw new BadException("Already Applied");
        }
        CompanyProfile company = companyProfileService.getCompanyProfileById(jobPostings.getCompanyId());
        if (ObjectUtils.isEmpty(company)) {
            throw new BadException("Company Profile not found " + jobPostings.getCompanyId());
        }
        JobApplications application = new JobApplications();
        application.setId(uniqueUtility.getNextNumber("JOB_APPLICATION", "job_application"));
        application.setJobId(jobId);
        application.setCandidateId(candidate.getId());
        application.setRecruiterId(jobPostings.getPostBy());
        application.setCompanyId(jobPostings.getCompanyId());
        application.setStatus(JobApplications.JobStatus.APPLIED);
        application.setAppliedAt(Instant.now());
        application.setCreatedAt(Instant.now());
        application.setCreatedBy(candidate.getId());
        application.setUpdatedAt(Instant.now());
        application.setCandidateName(candidate.getName());
        if (request.getUseSameEmail()) {
            application.setCandidateEmail(candidate.getEmail());
        } else {
            application.setCandidateEmail(request.getEmail());
        }
        application.setCompanyName(company.getBasicSetting().getCompanyName());
        application.setMobileNumber(request.getMobileNumber());
        application.setUpdatedBy(candidate.getId());
        if (!request.getUseSameResume()) {
            String resumeId = fileStorageService.storeFile(resume);
            application.setResumeId(resumeId);
        } else {
            application.setResumeId(candidate.getResumeId());
        }

        application = saveJobApplication(application);
        log.info("Saved Job applications :{}", application);
        return Boolean.TRUE;

    }

    public JobApplications saveJobApplication(JobApplications jobApplications) {
        log.info("Saving job application for the ID :{}", jobApplications.getId());
        return repository.save(jobApplications);
    }

    public List<JobPostingController.JobApplicationResponse> getAllJobApplications(JobPostings jobPostings) {
        log.info("Getting all job applications for the ID :{}", jobPostings.getId());
        List<JobApplications> jobApplications = repository.findByJobId(jobPostings.getId());
        if (jobApplications.isEmpty()) {
            return null;
        }
        List<JobPostingController.JobApplicationResponse> jobApplicationResponses = new ArrayList<>();
        for (JobApplications applications : jobApplications) {
            Candidate candidate = candidateService.getCandidateById(applications.getCandidateId());
            JobPostingController.JobApplicationResponse response = toJobApplicationResponse(applications, candidate);
            jobApplicationResponses.add(response);
        }
        return jobApplicationResponses;

    }

    public JobPostingController.JobApplicationResponse toJobApplicationResponse(JobApplications jobApplications, Candidate candidate) {
        JobPostingController.JobApplicationResponse response = new JobPostingController.JobApplicationResponse();
        response.setId(jobApplications.getId());
        response.setStatus(jobApplications.getStatus());
        response.setCandidateId(jobApplications.getCandidateId());
        response.setApplyDate(jobApplications.getAppliedAt());
        response.setCandidateName(jobApplications.getCandidateName());
        response.setResumeId(jobApplications.getResumeId());
        return response;
    }

    public List<CandidateApplyJobController.CandidateAppliedJobResponse> getAllAppliedJobsforCandidate(Candidate candidate) {
        log.info("Get all applied jobs for the candidate :{}", candidate.getId());
        List<JobApplications> applications = repository.findByCandidateId(candidate.getId());
        List<CandidateApplyJobController.CandidateAppliedJobResponse> list = new ArrayList<>();
        for (JobApplications jobApplications : applications) {
            CandidateApplyJobController.CandidateAppliedJobResponse response = new CandidateApplyJobController.CandidateAppliedJobResponse();
            response.setId(jobApplications.getId());
            response.setCandidateId(jobApplications.getCandidateId());
            response.setCandidateEmail(jobApplications.getCandidateEmail());
            response.setAppliedAt(jobApplications.getAppliedAt());
            JobPostings jobPostings = jobPostingService.getJobPostingById(jobApplications.getJobId());
            response.setJobId(jobApplications.getJobId());
            response.setResumeId(jobApplications.getResumeId());
            response.setJobProfile(jobPostings.getProfile());
            response.setJobType(jobPostings.getJobType());
            response.setTitle(jobPostings.getTitle());
            response.setSalaryRange(jobPostings.getSalaryRange());
            response.setCompanyName(jobPostings.getCompanyId());
            response.setCandidateMobileNumber(jobApplications.getMobileNumber());
            list.add(response);

        }
        return list;
    }

    public Integer totalJobAppliedCandidate(String jobId) {
        log.info("get number of applicants for the job ID :{}", jobId);
        List<JobApplications> applications = repository.findByJobId(jobId);
        return applications.size();
    }


}
