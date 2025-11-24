package com.aibackend.AiBasedEndtoEndSystem.controller;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.User;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;
import com.aibackend.AiBasedEndtoEndSystem.entity.Recruiter;
import com.aibackend.AiBasedEndtoEndSystem.service.RecruiterService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/hr")
@Slf4j
public class RecruiterController {
    @Autowired
    private RecruiterService recruiterService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PublicController publicController;

    @PostMapping("/create")
    public PublicController.UserResponse createNewHR(@RequestBody RecruiterRequest request) throws Exception {
        log.info("New Hr Details :{}", request);
        UserDTO userDTO = recruiterService.createNewRecruiter(request);
        userDTO.setRole("Recruiter");
        JwtUtil.Token token = jwtUtil.generateClientToken(userDTO);
        return publicController.toUserResponse(userDTO, token);
    }

    @PostMapping("/login")
    public PublicController.UserResponse createNewHR(@RequestBody PublicController.LoginRequest request) throws Exception {
        log.info("New Hr Details :{}", request);
        UserDTO userDTO = recruiterService.getUserLogin(request.getMobileNumber());
        userDTO.setRole("Recruiter");
        JwtUtil.Token token = jwtUtil.generateClientToken(userDTO);
        return publicController.toUserResponse(userDTO, token);
    }

    @GetMapping("/get")
    public UserDTO getUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7); // remove "Bearer "
        String id = jwtUtil.extractUserObjectId(token);
        log.info("Id for the candiate is :{}", id);
        log.info("Token for the Recruiter :{}", token);
        return SecurityUtils.getLoggedInUser(token, jwtUtil.getKey());
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
