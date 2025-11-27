package com.aibackend.AiBasedEndtoEndSystem.service;

import com.aibackend.AiBasedEndtoEndSystem.config.GoogleAuthConfig;
import com.aibackend.AiBasedEndtoEndSystem.controller.PublicController;
import com.aibackend.AiBasedEndtoEndSystem.dto.CandidateRequest;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.Candidate;
import com.aibackend.AiBasedEndtoEndSystem.entity.Recruiter;
import com.aibackend.AiBasedEndtoEndSystem.entity.User;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.repository.CandidateRepository;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import com.aibackend.AiBasedEndtoEndSystem.util.UniqueUtiliy;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class CandidateService {
    @Autowired
    private CandidateRepository candidateRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private UniqueUtiliy uniqueUtiliy;
    @Autowired
    private GoogleAuthConfig googleAuthConfig;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PublicController publicController;

    public UserDTO createNewCandidate(CandidateRequest request) {
        log.info("Create new candidate :{}", request);
        validateRequest(request);
        Optional<Candidate> existing = candidateRepository.findByMobileNumber(request.getMobileNumber());
        if (existing.isPresent()) {
            return userService.toCandidateDTO(existing.get());
        }
        Candidate candidate = new Candidate();
        candidate.setId(uniqueUtiliy.getNextNumber("CANDIDATE", "cd"));
        candidate.setName(request.getName());
        candidate.setEmail(request.getEmail());
        candidate.setMobileNumber(request.getMobileNumber());
        candidate.setAge(request.getAge());
        candidate.setGender(request.getGender());

        if (request.getLocation() != null) {
            Candidate.Location loc = new Candidate.Location();
            loc.setCity(request.getLocation().getCity());
            loc.setState(request.getLocation().getState());
            loc.setCountry(request.getLocation().getCountry());
            candidate.setLocation(loc);
        }
        candidate.setSkills(request.getSkills() == null ? new ArrayList<>() : new ArrayList<>(request.getSkills()));
        candidate.setExperienceYears(request.getExperienceYears());
        candidate.setHighestQualification(request.getHighestQualification());
        candidate.setCurrentJobRole(request.getCurrentJobRole());
        candidate.setCurrentCompany(null); // not provided in request; set later if needed
        candidate.setExpectedSalary(request.getExpectedSalary());
        candidate.setCurrentSalary(null); // unknown at signup
        candidate.setResumeUrl(request.getResumeUrl());
        candidate.setProfileImageUrl(request.getProfileImageUrl());
        candidateRepository.save(candidate);
        log.info("Saved Candidate :{}", candidate);
        return userService.toCandidateDTO(candidate);

    }

    private void validateRequest(CandidateRequest request) {


        if (ObjectUtils.isEmpty(request.getName())) throw new BadException("Name is required");
        if (ObjectUtils.isEmpty(request.getEmail())) throw new BadException("Email is required");
        if (ObjectUtils.isEmpty(request.getMobileNumber())) throw new BadException("Mobile number is required");
        if (request.getAge() == null || request.getAge() <= 0) throw new BadException("Valid age is required");
        if (ObjectUtils.isEmpty(request.getGender())) throw new BadException("Gender is required");
        if (request.getLocation() == null) throw new BadException("Location details are required");

        if (ObjectUtils.isEmpty(request.getLocation().getCity())) throw new BadException("City is required");
        if (ObjectUtils.isEmpty(request.getLocation().getState())) throw new BadException("State is required");
        if (ObjectUtils.isEmpty(request.getLocation().getCity())) throw new BadException("Country is required");

        if (request.getSkills() == null || request.getSkills().isEmpty())
            throw new BadException("At least one skill is required");

        if (request.getExperienceYears() == null || request.getExperienceYears() < 0)
            throw new BadException("Experience years must be valid");

        if (ObjectUtils.isEmpty(request.getHighestQualification()))
            throw new BadException("Highest qualification is required");

        if (ObjectUtils.isEmpty(request.getCurrentJobRole()))
            throw new BadException("Current job role is required");

        if (ObjectUtils.isEmpty(request.getResumeUrl()))
            throw new BadException("Resume URL is required");

        if (request.getExpectedSalary() == null || request.getExpectedSalary() <= 0)
            throw new BadException("Expected salary must be valid");

    }

    public UserDTO getCandidateByMobileNumber(PublicController.LoginRequest request) {
        log.info("Get the candidate for mobile Number :{}", request);
        Optional<Candidate> existing = null;
        if (!ObjectUtils.isEmpty(request.getEmail())) {
            existing = candidateRepository.findByEmail(request.getEmail());
        } else {
            existing = candidateRepository.findByMobileNumber(request.getMobileNumber());
        }
        if (existing.isPresent()) {
            return userService.toCandidateDTO(existing.get());
        }
        throw new BadException("No Candidate found with this " + request.getMobileNumber());
    }


    public Candidate findById(String id) {
        log.info("Get Candidate By id : {}", id);
        return candidateRepository.findById(id).orElse(null);
    }

    public Candidate findByEmail(String email) {
        Optional<Candidate> existing = candidateRepository.findByEmail(email);
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

            Candidate candidate;
            candidate = findByEmail(email);
            if (ObjectUtils.isEmpty(candidate)) {
                candidate.setId(uniqueUtiliy.getNextNumber("CANDIDATE", "cd"));
                candidate.setEmail(email);
                candidate = candidateRepository.save(candidate);
            }
            UserDTO userDTO = new UserDTO();
            userDTO.setId(candidate.getId());
            userDTO.setUserEmail(candidate.getEmail());
            userDTO.setRole(User.Role.RECRUITER.toString());
            userDTO.setUsername(candidate.getName());
            userDTO.setMobileNumber(candidate.getMobileNumber());
            JwtUtil.Token token = jwtUtil.generateClientToken(userDTO);
            return publicController.toUserResponse(userDTO, token);
        } catch (Exception e) {
            log.error("Error during Google OAuth callback: {}", e.getMessage(), e);
            throw new BadException("Some error occurred " + e.getMessage());
        }
    }

    public String getGoogleLoginUrlHost() {
        String clientId = googleAuthConfig.getClientId();
        String redirectUri = googleAuthConfig.getRedirectUriForCandidate();
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
}
