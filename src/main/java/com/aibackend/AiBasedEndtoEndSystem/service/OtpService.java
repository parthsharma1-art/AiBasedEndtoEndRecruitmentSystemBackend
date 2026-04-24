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
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OtpService {

    private static final String KEY_PREFIX = "auth:otp:";

    /** In-process OTP when Redis is down; only safe for a single app instance. */
    private static final ConcurrentHashMap<String, MemoryOtpEntry> MEMORY_OTP = new ConcurrentHashMap<>();

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

    /**
     * When true, if Redis is unreachable, OTP is stored in JVM memory (single-instance only).
     * Set {@code OTP_MEMORY_FALLBACK=true} on Render until {@code REDIS_URL} uses TLS ({@code rediss://}).
     */
    @Value("${app.auth.otp.memory-fallback-when-redis-unavailable:false}")
    private boolean memoryFallbackWhenRedisUnavailable;

    public String generateOtp(String email, String role) {
        String normalizedRole = requireRole(role);
        String normalizedEmail = normalizeEmail(email);
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        String key = buildRedisKey(normalizedRole, normalizedEmail);
        Duration ttl = Duration.ofMinutes(otpTtlMinutes);
        try {
            stringRedisTemplate.opsForValue().set(key, otp, ttl);
            MEMORY_OTP.remove(key);
            log.info("OTP generated and stored in Redis for role={} email={}", normalizedRole, normalizedEmail);
        } catch (Exception e) {
            log.error("Redis unavailable while storing OTP for role={} email={}: {}", normalizedRole, normalizedEmail, e.getMessage());
            if (memoryFallbackWhenRedisUnavailable) {
                MEMORY_OTP.put(key, new MemoryOtpEntry(otp, System.currentTimeMillis() + ttl.toMillis()));
                log.warn("OTP stored in memory fallback for key {}", key);
            } else {
                throw new BadException(
                        "Verification storage is unavailable. Ensure Redis is reachable "
                                + "(Upstash: use rediss:// in REDIS_URL). Or set OTP_MEMORY_FALLBACK=true for single-instance memory OTP.");
            }
        }
        return otp;
    }

    public UserDTO verifyOtp(String email, String otp, String role) {
        String normalizedRole = requireRole(role);
        String normalizedEmail = normalizeEmail(email);
        log.info("OTP verify for role={} email={}", normalizedRole, normalizedEmail);
        String key = buildRedisKey(normalizedRole, normalizedEmail);
        String stored = getStoredOtp(key);
        if (!StringUtils.hasText(stored) || !Objects.equals(stored.trim(), otp != null ? otp.trim() : null)) {
            throw new BadException("Invalid or expired otp");
        }
        deleteStoredOtp(key);

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

    private String getStoredOtp(String key) {
        try {
            String fromRedis = stringRedisTemplate.opsForValue().get(key);
            if (StringUtils.hasText(fromRedis)) {
                return fromRedis;
            }
        } catch (Exception e) {
            log.warn("Redis unavailable while reading OTP for key {}: {}", key, e.getMessage());
        }
        MemoryOtpEntry mem = MEMORY_OTP.get(key);
        if (mem == null) {
            return null;
        }
        if (mem.expiresAtMillis <= System.currentTimeMillis()) {
            MEMORY_OTP.remove(key);
            return null;
        }
        return mem.otp;
    }

    private void deleteStoredOtp(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis unavailable while deleting OTP key {}: {}", key, e.getMessage());
        }
        MEMORY_OTP.remove(key);
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

    private static final class MemoryOtpEntry {
        final String otp;
        final long expiresAtMillis;

        MemoryOtpEntry(String otp, long expiresAtMillis) {
            this.otp = otp;
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
