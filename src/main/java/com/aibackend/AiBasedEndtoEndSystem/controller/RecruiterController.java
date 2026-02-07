package com.aibackend.AiBasedEndtoEndSystem.controller;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;
import com.aibackend.AiBasedEndtoEndSystem.service.RecruiterService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/recruiter")
@Slf4j
public class RecruiterController {
    @Autowired
    private RecruiterService recruiterService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PublicController publicController;

    @PostMapping("/create")
    public PublicController.UserResponse createNewHR(@RequestBody RecruiterRequest request) {
        log.info("New Hr Details :{}", request);
        UserDTO userDTO = recruiterService.createNewRecruiter(request);
        userDTO.setRole("Recruiter");
        JwtUtil.Token token = jwtUtil.generateClientToken(userDTO);
        return publicController.toUserResponse(userDTO, token);
    }

    @PostMapping("/login")
    public PublicController.UserResponse createNewHR(@RequestBody PublicController.LoginRequest request) throws Exception {
        log.info("Recruiter login request :{}", request);
        UserDTO userDTO = recruiterService.getUserLogin(request);
        userDTO.setRole("Recruiter");
        JwtUtil.Token token = jwtUtil.generateClientToken(userDTO);
        return publicController.toUserResponse(userDTO, token);
    }

    @GetMapping("/get")
    public RecruiterResponse getUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        String id = jwtUtil.extractUserObjectId(token);
        UserDTO userDTO = SecurityUtils.getLoggedInUser(token, jwtUtil.getKey());
        return recruiterService.getRecruiterDetails(userDTO);
    }

    @GetMapping("/google/login")
    public PublicController.UserResponse googleCallback(@RequestParam("code") String code) throws IOException {
        log.info("Code :{}", code);
        return recruiterService.googleHostCallback(code);

    }

    @GetMapping("/google/login-url-recruiter")
    public void getGoogleLoginUrlHost(HttpServletResponse response) throws Exception {
        String googleAuthUrl = recruiterService.getGoogleLoginUrlHost();
        response.sendRedirect(googleAuthUrl);
    }

    @Data
    public static class RecruiterRequest {
        private String name;
        private String mobileNumber;
        private String email;
        private String companyName;
        private Integer age;
        private String state;
        private String country;
        private String designation;
    }

    @PostMapping("/logout")
    public Boolean logout(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return jwtUtil.invalidateToken(token);
            } else {
                return Boolean.FALSE;
            }
        } catch (Exception e) {
            log.info("Failed to logout");
            return Boolean.FALSE;

        }
    }

    @Data
    public static class RecruiterResponse {
        private String id;
        private String name;
        private String email;
        private String mobileNumber;
        private String companyId;
        private String companyName;

    }
}
