package com.aibackend.AiBasedEndtoEndSystem.controller;

import com.aibackend.AiBasedEndtoEndSystem.dto.CandidateRequest;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.Candidate;
import com.aibackend.AiBasedEndtoEndSystem.service.CandidateService;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
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

    @PostMapping("/create")
    public String createNewHR(@RequestBody CandidateRequest request) {
        log.info("New Hr Details :{}", request);
        UserDTO candidateDto = candidateService.createNewCandidate(request);
        return jwtUtil.generateToken(candidateDto);
    }

    @GetMapping("/{mobileNumber}")
    public Candidate getUser(@PathVariable String mobileNumber) {
        log.info("Get User by mobile number :{}", mobileNumber);
        return candidateService.getCandidateByMobileNumber(mobileNumber);
    }

}
