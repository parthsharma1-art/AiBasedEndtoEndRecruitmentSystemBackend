package com.aibackend.AiBasedEndtoEndSystem.controller;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import com.aibackend.AiBasedEndtoEndSystem.entity.Chat;
import com.aibackend.AiBasedEndtoEndSystem.service.ChatService;
import com.aibackend.AiBasedEndtoEndSystem.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.aibackend.AiBasedEndtoEndSystem.config.AuthAppConfig;
import com.aibackend.AiBasedEndtoEndSystem.dto.CandidateRequest;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.Candidate;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.service.CandidateService;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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
    @Autowired
    private ChatService chatService;
    @Autowired
    private NotificationService notificationService;

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
    public CandidateResponse getUser() {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        return toCandidateRespone(candidateService.findById(user.getId()));
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

    @PostMapping("/chats")
    public Chat createChat(@RequestBody RecruiterController.ChatRequest request) {
        log.info("Create or update Chats :{}", request);
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        return chatService.createOrUpdateChat(user,request, Chat.Source.CANDIDATE);
    }

    @GetMapping("/chats/{id}")
    public ChatService.ChatResponse getChats(@PathVariable String id) {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        log.info("Get chat for the user :{}",id);
        return chatService.getChats(user,id);
    }

    @GetMapping("/chats")
    public List<ChatService.ChatResponse> getChatsForRecruiter() {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        log.info("Get all chats for the user :{}",user);
        return chatService.getAllChats(user, Chat.Source.CANDIDATE);
    }

    @GetMapping("/notifications")
    public List<RecruiterController.NotificationResponse> getAllNotifications() {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        log.info("Get All notifications for the user :{}", user);
        return notificationService.getAllNotification(user);
    }

    @PostMapping("/notification/{id}")
    public Boolean getNotificattionById(@PathVariable String id) {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        log.info("Get notifications for the user :{}", user);
        return notificationService.markReadNotification(user,id);
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
    public void googleCallback(@RequestParam("code") String code, HttpServletResponse response) {
        log.info("Code :{}", code);
        try {
            PublicController.UserResponse dto = candidateService.googleHostCallback(code);
            String token = dto.getToken().getAuthKey();
            String id = dto.getId();
            String redirectUrl = authAppConfig.getFrontEndUrl() + "/candidate/google-success?token=" + token + "&id="
                    + id;
            response.sendRedirect(redirectUrl);
        } catch (Exception e) {
            log.error("Failed to create candidate via google login :{}", e.getMessage());
            throw new BadException("Failed to create Candidate via google login");
        }

    }

    @GetMapping("/google/login-url-candidate")
    public RecruiterController.GoogleAuthUrl getGoogleLoginUrlHost(HttpServletResponse response) {
        try {
            String googleAuthUrl = candidateService.getGoogleLoginUrlHost();
            RecruiterController.GoogleAuthUrl url = new RecruiterController.GoogleAuthUrl();
            url.setUrl(googleAuthUrl);
            return url;
        } catch (Exception e) {
            log.error("Failed to create candidate google login url :{}", e.getMessage());
            throw new BadException("Failed to create candidate google login url");
        }
    }

    @PostMapping("/logout")
    public Boolean logout(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                log.info("Token of logout :{}", token);
                return jwtUtil.invalidateToken(token);
            }
            return Boolean.FALSE;
        } catch (Exception e) {
            log.info("Failed to logout");
            return Boolean.FALSE;
        }
    }

    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PublicController.UserResponse updateCandidate(@ModelAttribute CandidateRequest request,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestPart(value = "resume", required = false) MultipartFile resume) {
        log.info("Update Candidate Details :{}", request);
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        UserDTO candidateDto = candidateService.updateCandidateDetails(user, request, profileImage, resume);
        candidateDto.setRole("Candidate");
        JwtUtil.Token jwtToken = jwtUtil.generateClientToken(candidateDto);
        return publicController.toUserResponse(candidateDto, jwtToken);
    }

}
