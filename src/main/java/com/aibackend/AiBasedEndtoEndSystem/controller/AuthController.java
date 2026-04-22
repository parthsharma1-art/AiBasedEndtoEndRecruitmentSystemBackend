package com.aibackend.AiBasedEndtoEndSystem.controller;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.service.BrevoEmailService;
import com.aibackend.AiBasedEndtoEndSystem.service.OtpService;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private BrevoEmailService emailService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PublicController publicController;

    @PostMapping("/send-otp")
    public Boolean sendOtp(@RequestBody EmailLoginRequest request) {
        try {
            String email = request.getEmail();
            log.info("Email is here :{}", email);
            String otp = otpService.generateOtp(email);
            emailService.sendHtmlEmail(email, otp);
            return Boolean.TRUE;
        } catch (Exception e) {
            log.error("Error occurred while sending otp :{}", e.getMessage());
            throw new BadException("Some error occurred while sending otp");
        }
    }

    @PostMapping("/verify-otp")
    public PublicController.UserResponse verify(@RequestBody VerifyRequest requset) {
        log.info("Request is this :{}", requset);
        UserDTO userDTO = otpService.verifyOtp(requset.getEmail(), requset.getOtp(), requset.getRole());
        if (ObjectUtils.isEmpty(userDTO)) {
            throw new BadException("Invalid otp");
        } else {
            JwtUtil.Token token = jwtUtil.generateClientToken(userDTO);
            log.info("user dto is here :{}", userDTO);
            return publicController.toUserResponse(userDTO, token);
        }
    }

    @Data
    public static class VerifyRequest {
        private String email;
        private String otp;
        private String role;

    }

    @Data
    public static class EmailLoginRequest {
        private String email;
    }
}
