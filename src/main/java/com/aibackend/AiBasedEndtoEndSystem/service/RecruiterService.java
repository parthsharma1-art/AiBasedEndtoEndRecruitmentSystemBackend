package com.aibackend.AiBasedEndtoEndSystem.service;

import com.aibackend.AiBasedEndtoEndSystem.config.GoogleAuthConfig;
import com.aibackend.AiBasedEndtoEndSystem.controller.CompanyProfileController;
import com.aibackend.AiBasedEndtoEndSystem.controller.PublicController;
import com.aibackend.AiBasedEndtoEndSystem.controller.RecruiterController;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.CompanyProfile;
import com.aibackend.AiBasedEndtoEndSystem.entity.Recruiter;
import com.aibackend.AiBasedEndtoEndSystem.entity.User;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.exception.HrException;
import com.aibackend.AiBasedEndtoEndSystem.repository.RecruiterRepository;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import com.aibackend.AiBasedEndtoEndSystem.util.UniqueUtiliy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class RecruiterService {
    @Autowired
    private RecruiterRepository repository;
    @Autowired
    private UserService userService;
    @Autowired
    private UniqueUtiliy uniqueUtiliy;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private GoogleAuthConfig googleAuthConfig;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PublicController publicController;
    @Autowired
    private CompanyProfileService companyProfileService;

    public UserDTO createNewRecruiter(RecruiterController.RecruiterRequest request) {
        Recruiter recruiter = new Recruiter();
        validateRequest(request);
        Optional<Recruiter> existing = repository.findByMobileNumber(request.getMobileNumber());
        if (existing.isPresent()) {
            return userService.toRecruiterDTO(existing.get());
        }
        recruiter.setId(uniqueUtiliy.getNextNumber("RECRUITER", "hr"));
        recruiter.setName(request.getName());
        recruiter.setCompanyName(request.getCompanyName());
        recruiter.setDesignation(request.getDesignation());
        recruiter.setEmail(request.getEmail());
        recruiter.setMobileNumber(request.getMobileNumber());
        recruiter = save(recruiter);
        CompanyProfileController.CompanyProfileResponse resoponse = companyProfileService.createCompanyProfileByRecruiter(recruiter);
        log.info("Company profile response : {}", resoponse);
        return userService.toRecruiterDTO(recruiter);
    }

    public Recruiter save(Recruiter recruiter) {
        log.info("Saving recruiter details ");
        return repository.save(recruiter);
    }

    public Recruiter getHrByMobileNumber(String mobileNumber) {
        log.info("Hr Mobile Number :{}", mobileNumber);
        Optional<Recruiter> hr = repository.findByMobileNumber(mobileNumber);
        if (hr.isPresent()) {
            return hr.get();
        }
        throw new HrException("Hr does not exist " + mobileNumber);
    }

    private void validateRequest(RecruiterController.RecruiterRequest request) {
        if (ObjectUtils.isEmpty(request.getName())) {
            throw new HrException("Recruiter Name is required");
        }
        if (ObjectUtils.isEmpty(request.getMobileNumber())) {
            throw new HrException("Recruiter Mobile Number is required");
        }
        if (ObjectUtils.isEmpty(request.getEmail())) {
            throw new HrException("Recruiter Email is required");
        }
        if (ObjectUtils.isEmpty(request.getAge())) {
            throw new HrException("Recruiter Age is required");
        }
        if (ObjectUtils.isEmpty(request.getState())) {
            throw new HrException("Recruiter State is required");
        }
        if (ObjectUtils.isEmpty(request.getCompanyName())) {
            throw new HrException("Recruiter Company Name is required");
        }
        if (ObjectUtils.isEmpty(request.getCountry())) {
            throw new HrException("Recruiter Country is required");
        }
        if (ObjectUtils.isEmpty(request.getDesignation())) {
            throw new HrException("Recruiter Designation is required");
        }
    }

    public Recruiter findById(String id) {
        log.info("Get Hr BY id : {}", id);
        return repository.findById(id).orElse(null);
    }

    public UserDTO getUserLogin(PublicController.LoginRequest request) {
        log.info("User mobile Number :{}", request);
        Optional<Recruiter> recruiter = null;
        if (!ObjectUtils.isEmpty(request.getMobileNumber())) {
            recruiter = repository.findByMobileNumber(request.getMobileNumber());
        } else {
            recruiter = repository.findByEmail(request.getEmail());
        }
        if (recruiter.isPresent()) {
            return userService.toRecruiterDTO(recruiter.get());
        }
        return null;

    }

    public Recruiter findByEmail(String email) {
        Optional<Recruiter> existing = repository.findByEmail(email);
        if (existing.isPresent()) {
            return existing.get();
        }
        return null;
    }

    public PublicController.UserResponse googleHostCallback(String code) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", googleAuthConfig.getClientId());
            params.add("client_secret", googleAuthConfig.getClientSecret());
            params.add("redirect_uri", googleAuthConfig.getRedirectUriForRecruiter());
            params.add("grant_type", "authorization_code");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> googleTokenRequest = new HttpEntity<>(params, headers);
            Map<String, Object> googleTokenResponse = restTemplate.postForObject(
                    "https://oauth2.googleapis.com/token", googleTokenRequest, Map.class);
            String accessToken = (String) googleTokenResponse.get("access_token");
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);
            ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v2/userinfo",
                    HttpMethod.GET,
                    userRequest,
                    Map.class);
            Map<String, Object> userInfo = userInfoResponse.getBody();
            String email = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");

            Recruiter recruiter;
            recruiter = findByEmail(email);
            if (ObjectUtils.isEmpty(recruiter)) {
                recruiter.setId(uniqueUtiliy.getNextNumber("RECRUITER", "hr"));
                recruiter.setEmail(email);
                recruiter = repository.save(recruiter);
            }
            UserDTO userDTO = new UserDTO();
            userDTO.setId(recruiter.getId());
            userDTO.setUserEmail(recruiter.getEmail());
            userDTO.setRole(User.Role.RECRUITER.toString());
            userDTO.setUsername(recruiter.getName());
            userDTO.setMobileNumber(recruiter.getMobileNumber());
            JwtUtil.Token token = jwtUtil.generateClientToken(userDTO);
            return publicController.toUserResponse(userDTO, token);
        } catch (Exception e) {
            log.error("Error during Google OAuth callback: {}", e.getMessage(), e);
            throw new BadException("Some error occurred " + e.getMessage());
        }
    }

    public String getGoogleLoginUrlHost() {
        String clientId = googleAuthConfig.getClientId();
        String redirectUri = googleAuthConfig.getRedirectUriForRecruiter();
        String responseType = "code";
        String scope = "openid profile email";
        String accessType = "offline";
        String prompt = "consent";
        String googleAuthUrlForHost = String.format(
                "https://accounts.google.com/o/oauth2/v2/auth" +
                        "?client_id=%s" +
                        "&redirect_uri=%s" +
                        "&response_type=%s" +
                        "&scope=%s" +
                        "&access_type=%s" +
                        "&prompt=%s",
                clientId,
                redirectUri,
                responseType,
                scope.replace(" ", "%20"),
                accessType,
                prompt);

        log.info("Generated Google Login URL: {}", googleAuthUrlForHost);
        return googleAuthUrlForHost;

    }

    public RecruiterController.RecruiterResponse getRecruiterDetails(UserDTO user) {
        log.info("Get details for the user :{}", user);
        Optional<Recruiter> optionalRecruiter = repository.findById(user.getId());
        if (optionalRecruiter.isEmpty()) {
            log.error("Recruiter not found for the id :{}", user.getId());
        }
        Recruiter recruiter = optionalRecruiter.get();
        CompanyProfile companyProfile = null;
        if (!ObjectUtils.isEmpty(recruiter.getCompanyId())) {
            companyProfile = companyProfileService.getCompanyProfileById(recruiter.getCompanyId());
        }
        RecruiterController.RecruiterResponse response = new RecruiterController.RecruiterResponse();
        response.setId(recruiter.getId());
        response.setName(recruiter.getName());
        response.setCompanyId(companyProfile.getId());
        response.setEmail(recruiter.getEmail());
        response.setMobileNumber(recruiter.getMobileNumber());
        response.setCompanyName(companyProfile.getBasicSetting().getCompanyName());
        return response;

    }
}
