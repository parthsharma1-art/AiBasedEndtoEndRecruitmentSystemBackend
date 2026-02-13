package com.aibackend.AiBasedEndtoEndSystem.controller;

import com.aibackend.AiBasedEndtoEndSystem.config.AuthAppConfig;
import com.aibackend.AiBasedEndtoEndSystem.config.GoogleAuthConfig;
import com.aibackend.AiBasedEndtoEndSystem.dto.CandidateRequest;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.Candidate;
import com.aibackend.AiBasedEndtoEndSystem.service.CandidateService;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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
    @Autowired
    private AuthAppConfig authAppConfig;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PublicController.UserResponse createNewCandidate(
            @ModelAttribute CandidateRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestPart(value = "resume", required = false) MultipartFile resume) {
        log.info("New Candidate Details :{}", request);
        UserDTO candidateDto = candidateService.createNewCandidate(request, profileImage, resume);
        candidateDto.setRole("Candidate");
        JwtUtil.Token token = jwtUtil.generateClientToken(candidateDto);
        return publicController.toUserResponse(candidateDto, token);
    }


    @PostMapping("/login")
    public PublicController.UserResponse login(@RequestBody PublicController.LoginRequest request) throws Exception {
        UserDTO user = candidateService.getCandidateByMobileNumber(request);
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
        response.setResumeId(candidate.getResumeId());
        response.setProfileImageId(candidate.getProfileImageId());
        return response;
    }

    @Data
    public static class CandidateResponse {
        private String id;
        private String name;
        private String email;
        private String mobileNumber;
        private String resumeId;
        private String profileImageId;
    }

    @GetMapping("/google/login")
    public void googleCallback(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
        log.info("Code :{}", code);
        PublicController.UserResponse dto = candidateService.googleHostCallback(code);
        String token = dto.getToken().getAuthKey();
        String id = dto.getId();
        String redirectUrl =
                authAppConfig.getFrontEndUrl() + "/candidate/google-success?token=" + token + "&id=" + id;
        response.sendRedirect(redirectUrl);

    }

    @GetMapping("/google/login-url-candidate")
    public RecruiterController.GoogleAuthUrl getGoogleLoginUrlHost(HttpServletResponse response) throws Exception {
        String googleAuthUrl = candidateService.getGoogleLoginUrlHost();
        RecruiterController.GoogleAuthUrl url = new RecruiterController.GoogleAuthUrl();
        url.setUrl(googleAuthUrl);
        return url;
    }

    @PostMapping("/logout")
    public Boolean logout(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                log.info("Token of logout :{}", token);
                return jwtUtil.invalidateToken(token);
            } else {
                return Boolean.FALSE;
            }
        } catch (Exception e) {
            log.info("Failed to logout");
            return Boolean.FALSE;
        }
    }


    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PublicController.UserResponse updateCandidate(@RequestHeader("Authorization") String authHeader,
                                                         @ModelAttribute CandidateRequest request,
                                                         @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
                                                         @RequestPart(value = "resume", required = false) MultipartFile resume) {
        log.info("Update Candidate Details :{}", request);
        String token = authHeader.substring(7);
        String id = jwtUtil.extractUserObjectId(token);
        UserDTO user = SecurityUtils.getLoggedInUser(token, jwtUtil.getKey());
        UserDTO candidateDto = candidateService.udpateCandidateDetails(user, request, profileImage, resume);
        candidateDto.setRole("Candidate");
        JwtUtil.Token jwtToken = jwtUtil.generateClientToken(candidateDto);
        return publicController.toUserResponse(candidateDto, jwtToken);
    }

}
