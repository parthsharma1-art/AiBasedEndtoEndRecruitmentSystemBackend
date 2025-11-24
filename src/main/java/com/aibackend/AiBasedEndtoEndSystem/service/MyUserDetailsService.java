package com.aibackend.AiBasedEndtoEndSystem.service;

import com.aibackend.AiBasedEndtoEndSystem.entity.Candidate;
import com.aibackend.AiBasedEndtoEndSystem.entity.Recruiter;
import com.aibackend.AiBasedEndtoEndSystem.entity.User;
import com.aibackend.AiBasedEndtoEndSystem.repository.CandidateRepository;
import com.aibackend.AiBasedEndtoEndSystem.repository.RecruiterRepository;
import com.aibackend.AiBasedEndtoEndSystem.repository.UserRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MyUserDetailsService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RecruiterService recruiterService;
    @Autowired
    private CandidateService candidateService;
    @Autowired
    private CandidateRepository candidateRepository;
    @Autowired
    private RecruiterRepository recruiterRepository;

    public User loadUserEntityById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
    }

    public Candidate loadCandidateById(String candidateId) {
        return candidateRepository.findById(candidateId).orElseThrow(() -> new UsernameNotFoundException("User not found with id :" + candidateId));
    }

    public Recruiter loadRecruiterById(String recruiterId) {
        return recruiterRepository.findById(recruiterId).orElseThrow(() -> new UsernameNotFoundException("User not found with id :" + recruiterId));
    }

    public User loadUserByUsername(String username) {
        return userRepository.findByMobileNumber(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }
}
