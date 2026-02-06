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

@CrossOrigin(origins = "*")
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
    public UserDTO getUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        String id = jwtUtil.extractUserObjectId(token);
        log.info("Id for the recruiter is :{}", id);
        log.info("Token for the Recruiter :{}", token);
        return SecurityUtils.getLoggedInUser(token, jwtUtil.getKey());
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
}
