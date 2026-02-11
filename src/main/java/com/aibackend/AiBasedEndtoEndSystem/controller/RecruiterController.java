package com.aibackend.AiBasedEndtoEndSystem.controller;

import com.aibackend.AiBasedEndtoEndSystem.config.AuthAppConfig;
import com.aibackend.AiBasedEndtoEndSystem.config.GoogleAuthConfig;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;
import com.aibackend.AiBasedEndtoEndSystem.service.RecruiterService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    @Autowired
    private AuthAppConfig authAppConfig;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PublicController.UserResponse createNewHR(
            @ModelAttribute RecruiterRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestPart(value = "idCard", required = false) MultipartFile idCard) {

        UserDTO userDTO = recruiterService.createNewRecruiter(request, profileImage, idCard);
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
        UserDTO userDTO = SecurityUtils.getLoggedInUser(token, jwtUtil.getKey());
        return recruiterService.getRecruiterDetails(userDTO);
    }

    @GetMapping("/google/login")
    public void googleCallback(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
        log.info("Code :{}", code);
        PublicController.UserResponse dto = recruiterService.googleHostCallback(code);
        String token = dto.getToken().getAuthKey();
        String id = dto.getId();
        String redirectUrl =
                authAppConfig.getFrontEndUrl() + "/google-success?token=" + token + "&id=" + id;
        response.sendRedirect(redirectUrl);

    }

    @GetMapping("/google/login-url-recruiter")
    public GoogleAuthUrl getGoogleLoginUrlHost(HttpServletResponse response) throws Exception {
        String googleAuthUrl = recruiterService.getGoogleLoginUrlHost();
        GoogleAuthUrl url = new GoogleAuthUrl();
        url.setUrl(googleAuthUrl);
        return url;
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

    @GetMapping("/overview")
    public RecruiterOverview getOverviewPage(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        String id = jwtUtil.extractUserObjectId(token);
        UserDTO userDTO = SecurityUtils.getLoggedInUser(token, jwtUtil.getKey());
        return recruiterService.getRecruiterOverview(userDTO);
    }

    @Data
    public static class RecruiterResponse {
        private String id;
        private String name;
        private String email;
        private String mobileNumber;
        private String companyId;
        private String companyName;
        private String profileImageUrl;

    }

    @Data
    public static class RecruiterOverview {
        private Integer totalJobs;
        private Integer totalCandidates;
        private Integer totalResumes;
        private Integer activeJobs;
    }

    @Data
    public static class GoogleAuthUrl {
        private String url;
    }
}
