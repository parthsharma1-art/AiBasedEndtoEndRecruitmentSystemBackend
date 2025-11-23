package com.aibackend.AiBasedEndtoEndSystem.service;

import com.aibackend.AiBasedEndtoEndSystem.dto.CandidateRequest;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.Candidate;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.repository.CandidateRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Optional;

@Service
@Slf4j
public class CandidateService {
    @Autowired
    private CandidateRepository candidateRepository;
    @Autowired
    private UserService userService;

    public UserDTO createNewCandidate(CandidateRequest request) {
        log.info("Create new candidate :{}", request);
        validateRequest(request);
        Optional<Candidate> existing = candidateRepository.findByMobileNumber(request.getMobileNumber());
        if (existing.isPresent()) {
            return userService.toCandidateDTO(existing.get());
        }
        Candidate candidate = new Candidate();
        candidate.setName(request.getName());
        candidate.setEmail(request.getEmail());
        candidate.setMobileNumber(request.getMobileNumber());
        candidate.setAge(request.getAge());
        candidate.setGender(request.getGender());

        if (request.getLocation() != null) {
            Candidate.Location loc = new Candidate.Location();
            loc.setCity(request.getLocation().getCity());
            loc.setState(request.getLocation().getState());
            loc.setCountry(request.getLocation().getCountry());
            candidate.setLocation(loc);
        }
        candidate.setSkills(request.getSkills() == null ? new ArrayList<>() : new ArrayList<>(request.getSkills()));
        candidate.setExperienceYears(request.getExperienceYears());
        candidate.setHighestQualification(request.getHighestQualification());
        candidate.setCurrentJobRole(request.getCurrentJobRole());
        candidate.setCurrentCompany(null); // not provided in request; set later if needed
        candidate.setExpectedSalary(request.getExpectedSalary());
        candidate.setCurrentSalary(null); // unknown at signup
        candidate.setResumeUrl(request.getResumeUrl());
        candidate.setProfileImageUrl(request.getProfileImageUrl());
        candidateRepository.save(candidate);
        log.info("Saved Candidate :{}", candidate);
        return userService.toCandidateDTO(candidate);

    }

    private void validateRequest(CandidateRequest request) {


        if (ObjectUtils.isEmpty(request.getName())) throw new BadException("Name is required");
        if (ObjectUtils.isEmpty(request.getEmail())) throw new BadException("Email is required");
        if (ObjectUtils.isEmpty(request.getMobileNumber())) throw new BadException("Mobile number is required");
        if (request.getAge() == null || request.getAge() <= 0) throw new BadException("Valid age is required");
        if (ObjectUtils.isEmpty(request.getGender())) throw new BadException("Gender is required");
        if (request.getLocation() == null) throw new BadException("Location details are required");

        if (ObjectUtils.isEmpty(request.getLocation().getCity())) throw new BadException("City is required");
        if (ObjectUtils.isEmpty(request.getLocation().getState())) throw new BadException("State is required");
        if (ObjectUtils.isEmpty(request.getLocation().getCity())) throw new BadException("Country is required");

        if (request.getSkills() == null || request.getSkills().isEmpty())
            throw new BadException("At least one skill is required");

        if (request.getExperienceYears() == null || request.getExperienceYears() < 0)
            throw new BadException("Experience years must be valid");

        if (ObjectUtils.isEmpty(request.getHighestQualification()))
            throw new BadException("Highest qualification is required");

        if (ObjectUtils.isEmpty(request.getCurrentJobRole()))
            throw new BadException("Current job role is required");

        if (ObjectUtils.isEmpty(request.getResumeUrl()))
            throw new BadException("Resume URL is required");

        if (request.getExpectedSalary() == null || request.getExpectedSalary() <= 0)
            throw new BadException("Expected salary must be valid");

    }

    public Candidate getCandidateByMobileNumber(String mobileNumber) {
        log.info("Get the candidate for mobile Number :{}", mobileNumber);
        Optional<Candidate> existing = candidateRepository.findByMobileNumber(mobileNumber);
        if (existing.isPresent()) {
            return existing.get();
        }
        throw new BadException("No Candidate found with this " + mobileNumber);
    }

    public Candidate findById(String id) {
        log.info("Get Hr BY id : {}", id);
        ObjectId objectId = new ObjectId(id);
        return candidateRepository.findById(objectId).orElse(null);
    }

}
