package com.aibackend.AiBasedEndtoEndSystem.service;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.Candidate;
import com.aibackend.AiBasedEndtoEndSystem.entity.Recruiter;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

@Service
@Slf4j
public class OtpService {

    private static final String KEY_PREFIX = "auth:otp:";

    @Autowired
    private RecruiterService recruiterService;
    @Autowired
    private UserService userService;
    @Autowired
    private CandidateService candidateService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${app.auth.otp.ttl-minutes:10}")
    private long otpTtlMinutes;

    public String generateOtp(String email, String role) {
        String normalizedRole = requireRole(role);
        String normalizedEmail = normalizeEmail(email);
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        String key = buildRedisKey(normalizedRole, normalizedEmail);
        stringRedisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(otpTtlMinutes));
        log.info("OTP generated and stored in Redis for role={} email={}", normalizedRole, normalizedEmail);
        return otp;
    }

    public UserDTO verifyOtp(String email, String otp, String role) {
        String normalizedRole = requireRole(role);
        String normalizedEmail = normalizeEmail(email);
        log.info("OTP verify for role={} email={}", normalizedRole, normalizedEmail);
        String key = buildRedisKey(normalizedRole, normalizedEmail);
        String stored = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(stored) || !Objects.equals(stored.trim(), otp != null ? otp.trim() : null)) {
            throw new BadException("Invalid or expired otp");
        }
        stringRedisTemplate.delete(key);

        if ("recruiter".equalsIgnoreCase(normalizedRole)) {
            Recruiter recruiter = recruiterService.findByEmailIgnoreCaseForOtp(normalizedEmail);
            if (!ObjectUtils.isEmpty(recruiter)) {
                UserDTO dto = userService.toRecruiterDTO(recruiter);
                dto.setRole("recruiter");
                return dto;
            }
            throw new BadException("Invalid otp");
        }
        if ("candidate".equalsIgnoreCase(normalizedRole)) {
            Candidate candidate = candidateService.findByEmailIgnoreCaseForOtp(normalizedEmail);
            if (!ObjectUtils.isEmpty(candidate)) {
                UserDTO dto = userService.toCandidateDTO(candidate);
                dto.setRole("candidate");
                return dto;
            }
            throw new BadException("Invalid otp");
        }
        throw new BadException("Please try again later");
    }

    private static String buildRedisKey(String normalizedRole, String normalizedEmail) {
        return KEY_PREFIX + normalizedRole + ":" + normalizedEmail;
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new BadException("Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String requireRole(String role) {
        if (!StringUtils.hasText(role)) {
            throw new BadException("Role is required (candidate or recruiter)");
        }
        String r = role.trim().toLowerCase(Locale.ROOT);
        if (!"candidate".equals(r) && !"recruiter".equals(r)) {
            throw new BadException("Role must be candidate or recruiter");
        }
        return r;
    }
}
