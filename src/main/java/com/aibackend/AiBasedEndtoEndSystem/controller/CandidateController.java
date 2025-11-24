package com.aibackend.AiBasedEndtoEndSystem.controller;

import com.aibackend.AiBasedEndtoEndSystem.dto.CandidateRequest;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.Candidate;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.service.CandidateService;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/candidate")
@Slf4j
public class CandidateController {

    @Autowired
    private CandidateService candidateService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PublicController publicController;

    @PostMapping("/create")
    public PublicController.UserResponse createNewHR(@RequestBody CandidateRequest request) throws Exception {
        log.info("New Hr Details :{}", request);
        UserDTO candidateDto = candidateService.createNewCandidate(request);
        candidateDto.setRole("Candidate");
        JwtUtil.Token token = jwtUtil.generateClientToken(candidateDto);
        return publicController.toUserResponse(candidateDto, token);
    }

    @PostMapping("/login")
    public PublicController.UserResponse login(@RequestBody PublicController.LoginRequest login) throws Exception {
        UserDTO user = candidateService.getCandidateByMobileNumber(login.getMobileNumber());
        user.setRole("Candidate");
        JwtUtil.Token token = jwtUtil.generateClientToken(user);
        log.info("The token generated for login :{}", token);
        return publicController.toUserResponse(user, token);
    }

    @GetMapping("/get")
    public CandidateResponse getUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String id = jwtUtil.extractUserObjectId(token);
        return toCandidateRespone(candidateService.findById(id));
    }

    public CandidateResponse toCandidateRespone(Candidate candidate) {
        CandidateResponse response = new CandidateResponse();
        response.setId(candidate.getId());
        response.setName(candidate.getName());
        response.setEmail(candidate.getEmail());
        response.setMobileNumber(candidate.getMobileNumber());
        response.setResumeUrl(candidate.getResumeUrl());
        return response;
    }

    @Data
    public static class CandidateResponse {
        private String id;
        private String name;
        private String email;
        private String mobileNumber;
        private String resumeUrl;
    }

}
