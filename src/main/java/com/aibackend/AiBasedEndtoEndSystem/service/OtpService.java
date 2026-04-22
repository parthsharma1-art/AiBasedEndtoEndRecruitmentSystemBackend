package com.aibackend.AiBasedEndtoEndSystem.service;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.Candidate;
import com.aibackend.AiBasedEndtoEndSystem.entity.Recruiter;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class OtpService {
    @Autowired
    private RecruiterService recruiterService;
    @Autowired
    private UserService userService;
    @Autowired
    private CandidateService candidateService;

    private final Map<String, String> otpStore = new HashMap<>();

    public String generateOtp(String email) {
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        otpStore.put(email, otp);
        log.info("The otp is :{}", otp);
        return otp;
    }

    public UserDTO verifyOtp(String email, String otp, String role) {
        log.info("Validating the request here :{} and role :{}", email, role);
        Boolean value = otp.equals(otpStore.get(email));
        if (value) {
            log.info("Validating for the role now :{}",role);
            if (role.toLowerCase().equals("recruiter")) {
                Recruiter recruiter = recruiterService.findByEmail(email);
                if (!ObjectUtils.isEmpty(recruiter)) {
                    UserDTO dto = userService.toRecruiterDTO(recruiter);
                    dto.setRole("recruiter");
                    return dto;
                }
                throw new BadException("Invalid otp");
            } else if (role.toLowerCase().equals("candidate")) {
                Candidate candidate = candidateService.findByEmail(email);
                if (!ObjectUtils.isEmpty(candidate)) {
                    UserDTO dto = userService.toCandidateDTO(candidate);
                    dto.setRole("candidate");
                    return dto;
                }
                throw new BadException("Invalid otp");
            }
        }
        throw new BadException("Please try again later");
    }
}
