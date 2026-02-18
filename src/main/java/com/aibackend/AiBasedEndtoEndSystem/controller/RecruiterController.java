package com.aibackend.AiBasedEndtoEndSystem.controller;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.io.IOException;
import java.util.List;

import com.aibackend.AiBasedEndtoEndSystem.entity.Chat;
import com.aibackend.AiBasedEndtoEndSystem.service.ChatService;
import com.aibackend.AiBasedEndtoEndSystem.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.aibackend.AiBasedEndtoEndSystem.config.AuthAppConfig;
import com.aibackend.AiBasedEndtoEndSystem.controller.CandidateController.CandidateResponse;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.service.RecruiterService;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import com.aibackend.AiBasedEndtoEndSystem.util.SecurityUtils;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

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
    @Autowired
    private ChatService chatService;
    @Autowired
    private NotificationService notificationService;

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
    public PublicController.UserResponse createNewHR(@RequestBody PublicController.LoginRequest request)
            throws Exception {
        log.info("Recruiter login request :{}", request);
        UserDTO userDTO = recruiterService.getUserLogin(request);
        userDTO.setRole("Recruiter");
        JwtUtil.Token token = jwtUtil.generateClientToken(userDTO);
        log.info("user dto is here :{}", userDTO);
        return publicController.toUserResponse(userDTO, token);
    }

    @GetMapping("/get")
    public RecruiterResponse getUser() {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        return recruiterService.getRecruiterDetails(user);
    }

    @GetMapping("/candidate/get-all")
    public List<CandidateResponse> getAllCandidate() {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null) {
            log.error("user not found for the ID ", user);
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        }
        return recruiterService.getAllCandidate(user);
    }

    @GetMapping("/candidate/{id}")
    public CandidateDetails getCandidateDetailsById(@PathVariable String id) {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null) {
            log.error("user not found for the ID ", user);
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        }
        return recruiterService.getCandidateDetailsById(user, id);
    }

    @GetMapping("/google/login")
    public void googleCallback(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
        log.info("Code :{}", code);
        PublicController.UserResponse dto = recruiterService.googleHostCallback(code);
        String token = dto.getToken().getAuthKey();
        String id = dto.getId();
        String redirectUrl = authAppConfig.getFrontEndUrl() + "/google-success?token=" + token + "&id=" + id;
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
    public RecruiterOverview getOverviewPage() {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        return recruiterService.getRecruiterOverview(user);
    }

    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PublicController.UserResponse updateCandidate(@RequestHeader("Authorization") String authHeader,
                                                         @ModelAttribute RecruiterRequest request,
                                                         @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
                                                         @RequestPart(value = "resume", required = false) MultipartFile idCard) {
        log.info("Update Candidate Details :{}", request);
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        UserDTO recruiterDTO = recruiterService.updateRecruiterDetails(request, profileImage, idCard, user);
        recruiterDTO.setRole("Recruiter");
        JwtUtil.Token jwtToken = jwtUtil.generateClientToken(recruiterDTO);
        return publicController.toUserResponse(recruiterDTO, jwtToken);
    }

    @PostMapping("/chats")
    public Chat createChat(@RequestBody ChatRequest request) {
        log.info("Create or update Chats :{}", request);
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        return chatService.createOrUpdateChat(user, request, Chat.Source.RECRUITER);
    }

    @GetMapping("/chats/{id}")
    public ChatService.ChatResponse getChats(@PathVariable String id) {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        log.info("Get chat by id :{} for the user  :{}", id, user);
        return chatService.getChats(user, id);
    }

    @GetMapping("/chats")
    public List<ChatService.ChatResponse> getChatsForRecruiter() {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        log.info("Get chats for the recruiter :{}", user);
        return chatService.getAllChats(user, Chat.Source.RECRUITER);
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
    public Boolean markNotificationRead(@PathVariable String id) {
        UserDTO user = SecurityUtils.getLoggedInUser();
        if (user == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Not authenticated");
        log.info("Get notifications for the user :{}", user);
        return notificationService.markReadNotification(user,id);
    }

    @PostMapping("/notification/mark-all-read")
    public Boolean markAllReadyNotification() {
        UserDTO userDTO = SecurityUtils.getLoggedInUser();
        return notificationService.markAllRead(userDTO);
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
        private String state;
        private String country;
        private Integer age;
        private String designation;

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

    @Data
    public static class CandidateDetails {
        private String id;
        private String name;
        private String email;
        private String mobileNumber;
        private Integer age;
        private String gender;
        private List<String> skills;
        private String highestQualification;
        private String profileImageId;
        private String resumeId;
        private String location;
    }

    @Data
    public static class ChatRequest {
        private String recruiterId;
        private String candidateId;
        private String message;
    }

    @Data
    public static class NotificationResponse {
        private String id;
        private String recruiterId;
        private Boolean read;
        private String message;
        private String title;
        private String relativeId;
    }
}
