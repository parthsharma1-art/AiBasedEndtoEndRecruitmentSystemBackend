package com.aibackend.AiBasedEndtoEndSystem.service;

import com.aibackend.AiBasedEndtoEndSystem.controller.CompanyProfileController;
import com.aibackend.AiBasedEndtoEndSystem.controller.CompanyProfileController.CompanyProfileResponse;
import com.aibackend.AiBasedEndtoEndSystem.controller.CompanyProfileController.CompanyProfileRequest;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.CompanyProfile;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobPostings;
import com.aibackend.AiBasedEndtoEndSystem.entity.Recruiter;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.repository.CompanyProfileRepository;
import com.aibackend.AiBasedEndtoEndSystem.util.UniqueUtiliy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
public class CompanyProfileService {
    @Autowired
    private CompanyProfileRepository repository;
    @Autowired
    @Lazy
    private RecruiterService recruiterService;
    @Autowired
    private UniqueUtiliy uniqueUtiliy;
    @Autowired
    private JobPostingService jobPostingService;

    public CompanyProfileResponse createCompanyProfile(Recruiter recruiter) {
        log.info("Creating new company profile for recruiter id :{}", recruiter.getId());
        Optional<CompanyProfile> exist = repository.getCompanyProfileByRecruiterId(recruiter.getId());
        if (exist.isPresent()) {
            return null;
        }
        CompanyProfile profile = new CompanyProfile();
        profile.setId(uniqueUtiliy.getNextNumber("COMPANY_PROFILE", "companyProfile"));
        profile.setRecruiterId(recruiter.getId());
        profile.setCreatedAt(Instant.now());
        profile.setCreatedBy(recruiter.getId());
        profile = save(profile);
        recruiter.setCompanyId(profile.getId());
        recruiterService.save(recruiter);
        return toResponse(profile);
    }

    public CompanyProfileResponse updateCompanyProfile(CompanyProfileRequest request, UserDTO user) {
        log.info("Updating profile for the user :{}", user);
        Optional<CompanyProfile> existCompanyProfile = repository.getCompanyProfileByRecruiterId(user.getId());
        if (existCompanyProfile.isEmpty()) {
            log.info("No company profile found ");
            throw new RuntimeException("No company profile found for the user " + user.getId());
        }
        CompanyProfile companyProfile = existCompanyProfile.get();
        boolean hasChanges = false;
        if (!ObjectUtils.isEmpty(request.getBasicSetting())) {
            companyProfile.setBasicSetting(request.getBasicSetting());
            hasChanges = true;
        }
        if (!ObjectUtils.isEmpty(request.getSocialLinks())) {
            companyProfile.setSocialLinks(request.getSocialLinks());
            hasChanges = true;
        }
        if (!ObjectUtils.isEmpty(request.getContactDetails())) {
            companyProfile.setContactDetails(request.getContactDetails());
            hasChanges = true;
        }
        if (hasChanges) {
            companyProfile = save(companyProfile);
        }
        return toResponse(companyProfile);
    }

    public CompanyProfile save(CompanyProfile companyProfile) {
        log.info("Saving company profile");
        return repository.save(companyProfile);
    }

    public CompanyProfileResponse toResponse(CompanyProfile companyProfile) {
        log.info("Converting save file to Response :{}", companyProfile.getId());
        CompanyProfileResponse response = new CompanyProfileResponse();
        response.setId(companyProfile.getId());
        response.setContactDetails(companyProfile.getContactDetails());
        response.setBasicSetting(companyProfile.getBasicSetting());
        response.setSocialLinks(companyProfile.getSocialLinks());
        response.setRecruiterId(companyProfile.getRecruiterId());
        response.setCreatedAt(companyProfile.getCreatedAt());
        response.setCreatedBy(companyProfile.getCreatedBy());
        return response;
    }

    public CompanyProfileResponse getCompanyProfileDetails(UserDTO user) {
        log.info("get company profile details for the user :{}", user);
        Optional<CompanyProfile> existCompanyProfile = repository.getCompanyProfileByRecruiterId(user.getId());
        if (existCompanyProfile.isEmpty()) {
            return null;
        }
        CompanyProfile companyProfile = existCompanyProfile.get();
        return toResponse(companyProfile);
    }

    public CompanyProfileResponse getDetailsByCompanyDomain(String domain) {
        log.info("Getting the details by domain :{}", domain);
        Optional<CompanyProfile> exist = repository.findByBasicSettingCompanyDomain(domain);
        if (exist.isEmpty()) {
            log.info("No Company profile found for this domain :{}", domain);
            return null;
        }
        return toResponse(exist.get());
    }

    public CompanyProfile getCompanyProfileByRecruiterId(String recruiterId) {
        log.info("Get company profile for the recruiter :{}", recruiterId);
        Optional<CompanyProfile> companyProfile = repository.getCompanyProfileByRecruiterId(recruiterId);
        if(companyProfile.isEmpty()){
            return null;
        }
        return companyProfile.get();
    }

    public CompanyProfileController.JobPostingsResponse createJobPosting(UserDTO user, CompanyProfileController.JobPostingsRequest request) {
        log.info("Creating job for the recruiter :{}", user);
        Optional<CompanyProfile> exist = repository.getCompanyProfileByRecruiterId(user.getId());
        if (exist.isEmpty()) {
            throw new BadException("Company Profile not found for the user " + user.getId());
        }
        CompanyProfile companyProfile = exist.get();
        log.info("Company profile id :{}", companyProfile.getId());
        log.info("Request coming from frontend :{}", request);
        JobPostings jopPosting = jobPostingService.createJob(request, companyProfile);
        return new CompanyProfileController.JobPostingsResponse(jopPosting);
    }
}
