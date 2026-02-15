package com.aibackend.AiBasedEndtoEndSystem.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.aibackend.AiBasedEndtoEndSystem.config.GoogleAuthConfig;
import com.aibackend.AiBasedEndtoEndSystem.controller.CandidateController.CandidateResponse;
import com.aibackend.AiBasedEndtoEndSystem.controller.CompanyProfileController;
import com.aibackend.AiBasedEndtoEndSystem.controller.PublicController;
import com.aibackend.AiBasedEndtoEndSystem.controller.RecruiterController;
import com.aibackend.AiBasedEndtoEndSystem.controller.RecruiterController.CandidateDetails;
import com.aibackend.AiBasedEndtoEndSystem.controller.RecruiterController.RecruiterOverview;
import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.entity.Candidate;
import com.aibackend.AiBasedEndtoEndSystem.entity.CompanyProfile;
import com.aibackend.AiBasedEndtoEndSystem.entity.JobPostings;
import com.aibackend.AiBasedEndtoEndSystem.entity.Recruiter;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.exception.HrException;
import com.aibackend.AiBasedEndtoEndSystem.repository.RecruiterRepository;
import com.aibackend.AiBasedEndtoEndSystem.util.JwtUtil;
import com.aibackend.AiBasedEndtoEndSystem.util.UniqueUtility;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RecruiterService {
    @Autowired
    private RecruiterRepository repository;
    @Autowired
    private UserService userService;
    @Autowired
    private UniqueUtility uniqueUtility;
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
    @Autowired
    private JobPostingService jobPostingService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private CandidateService candidateService;

    public UserDTO createNewRecruiter(RecruiterController.RecruiterRequest request, MultipartFile profileImage,
            MultipartFile idCard) {
        Recruiter recruiter = new Recruiter();
        validateRequest(request);
        Optional<Recruiter> existing = repository.findByMobileNumber(request.getMobileNumber());
        if (existing.isPresent()) {
            return userService.toRecruiterDTO(existing.get());
        }
        recruiter.setId(uniqueUtility.getNextNumber("RECRUITER", "hr"));
        recruiter.setName(request.getName());
        recruiter.setCompanyName(request.getCompanyName());
        recruiter.setDesignation(request.getDesignation());
        recruiter.setEmail(request.getEmail());
        recruiter.setMobileNumber(request.getMobileNumber());
        recruiter.setAge(request.getAge());
        recruiter.setState(request.getState());
        recruiter.setCountry(recruiter.getCountry());

        // Save profile image
        if (profileImage != null && !profileImage.isEmpty()) {
            String profileImageId = fileStorageService.storeFile(profileImage);
            recruiter.setProfileImageId(profileImageId);
        }

        // Save ID card
        if (idCard != null && !idCard.isEmpty()) {
            String idCardId = fileStorageService.storeFile(idCard);
            recruiter.setIdCardFileId(idCardId);
        }

        recruiter = save(recruiter);
        CompanyProfileController.CompanyProfileResponse resoponse = companyProfileService
                .createCompanyProfileByRecruiter(recruiter);
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
                recruiter = new Recruiter();
                recruiter.setId(uniqueUtility.getNextNumber("RECRUITER", "hr"));
                recruiter.setEmail(email);
                recruiter.setName(name);
                recruiter = repository.save(recruiter);
            }
            UserDTO userDTO = new UserDTO();
            userDTO.setId(recruiter.getId());
            userDTO.setUserEmail(recruiter.getEmail());
            userDTO.setRole("Recruiter");
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

        Recruiter recruiter = repository.findById(user.getId())
                .orElseThrow(() -> new BadException("Recruiter not found"));
        if (!ObjectUtils.isEmpty(recruiter) && !recruiter.getId().equals(user.getId())) {
            log.error("Unauthorized Access to the Recruiter :{}", user.getId());
            throw new BadException("Unauthorized Access to the Recruiter " + user.getId());
        }
        RecruiterController.RecruiterResponse response = new RecruiterController.RecruiterResponse();
        response.setId(recruiter.getId());
        response.setName(recruiter.getName());
        response.setEmail(recruiter.getEmail());
        response.setMobileNumber(recruiter.getMobileNumber());
        response.setAge(recruiter.getAge());
        response.setState(recruiter.getState());
        response.setCountry(recruiter.getCountry());
        if (recruiter.getProfileImageId() != null) {
            response.setProfileImageUrl("/file/" + recruiter.getProfileImageId());
        }
        if (recruiter.getCompanyId() != null) {
            CompanyProfile companyProfile = companyProfileService.getCompanyProfileById(recruiter.getCompanyId());
            if (companyProfile != null) {
                response.setCompanyId(companyProfile.getId());
                if (companyProfile.getBasicSetting() != null) {
                    response.setCompanyName(companyProfile.getBasicSetting().getCompanyName());
                } else {
                    log.warn("BasicSetting is null for company {}", companyProfile.getId());
                }
            }
        }
        response.setDesignation(recruiter.getDesignation());
        log.info("Response :{}", response);
        return response;
    }

    public RecruiterOverview getRecruiterOverview(UserDTO user) {
        log.info("Get overview page details for the user :{}", user);
        Recruiter recruiter = findById(user.getId());
        if (ObjectUtils.isEmpty(recruiter)) {
            log.info("No recruiter found");
            throw new BadException("Recruiter not found with this id " + user.getId());
        }
        if (!ObjectUtils.isEmpty(recruiter) && !recruiter.getId().equals(user.getId())) {
            log.error("Unauthorize access to the Recruiter Dashboard:{}", user);
            throw new BadException("Unauthorize access to the Recruiter Dashboard ");
        }
        CompanyProfile companyProfile = companyProfileService.getCompanyProfileByRecruiterId(recruiter.getId());
        if (ObjectUtils.isEmpty(companyProfile)) {
            log.error("No Company profile found for the recruiter :{}", recruiter.getId());
            throw new BadException("Company profile not found for the recruiter " + recruiter.getId());
        }
        List<JobPostings> jobPostings = jobPostingService.getAllJobPostings(recruiter);
        RecruiterOverview recruiterOverview = new RecruiterOverview();
        Integer activeJobs = 0;
        Integer totalJobs = 0;
        for (JobPostings postings : jobPostings) {
            if (postings.isActive()) {
                activeJobs++;
            }
            totalJobs++;
        }
        recruiterOverview.setActiveJobs(activeJobs);
        recruiterOverview.setTotalJobs(totalJobs);
        return recruiterOverview;
    }

    public Recruiter getRecruiterById(String id) {
        log.info("Get recruiter id :{}", id);
        Optional<Recruiter> recruiter = repository.findById(id);
        if (recruiter.isEmpty()) {
            return null;
        }
        return recruiter.get();
    }

    public UserDTO updateRecruiterDetails(RecruiterController.RecruiterRequest request, MultipartFile profileImage,
            MultipartFile idCard, UserDTO userDTO) {
        log.info("Logged In Recruiter Id is :{}", userDTO.getId());
        validateRequest(request);
        Recruiter recruiter = getRecruiterById(userDTO.getId());
        if (ObjectUtils.isEmpty(recruiter)) {
            log.error("Recruiter not found for the id :{}", userDTO.getId());
            throw new BadException("Recruiter not found for the id " + userDTO.getId());
        }
        if (!ObjectUtils.isEmpty(recruiter) && !recruiter.getId().equals(userDTO.getId())) {
            log.error("Unauthorize access to the user :{}", userDTO.getId());
            throw new BadException("Unauthorize access to the user " + userDTO.getId());
        }
        recruiter.setName(request.getName());
        recruiter.setCompanyName(request.getCompanyName());
        recruiter.setDesignation(request.getDesignation());
        recruiter.setEmail(request.getEmail());
        recruiter.setMobileNumber(request.getMobileNumber());
        recruiter.setAge(request.getAge());
        recruiter.setState(request.getState());
        recruiter.setCountry(request.getCountry());
        recruiter.setDesignation(request.getDesignation());

        if (profileImage != null && !profileImage.isEmpty()) {
            if (!ObjectUtils.isEmpty(recruiter.getProfileImageId())) {
                log.info("Deleting the recruiter profile image");
                fileStorageService.deleteFile(recruiter.getProfileImageId());
            }
        }
        if (idCard != null && !idCard.isEmpty()) {
            if (!ObjectUtils.isEmpty(recruiter.getIdCardFileId())) {
                log.info("Deleting the recruiter id card image");
                fileStorageService.deleteFile(recruiter.getIdCardFileId());
            }
        }
        if (profileImage != null && !profileImage.isEmpty()) {
            String profileImageId = fileStorageService.storeFile(profileImage);
            recruiter.setProfileImageId(profileImageId);
        }
        if (idCard != null && !idCard.isEmpty()) {
            log.info("id card check please check");
            String idCardId = fileStorageService.storeFile(idCard);
            recruiter.setIdCardFileId(idCardId);
        }
        recruiter = save(recruiter);
        CompanyProfileController.CompanyProfileResponse resoponse = companyProfileService
                .createCompanyProfileByRecruiter(recruiter);
        log.info("Recruiter response : {}", resoponse);
        return userService.toRecruiterDTO(recruiter);
    }

    public List<CandidateResponse> getAllCandidate(UserDTO user) {
        log.info("Get details for the user :{}", user);
        List<CandidateResponse> responses = new ArrayList<>();
        List<Candidate> candidates = candidateService.getAllCandidate();
        if (candidates.isEmpty()) {
            log.info("No candidate found :{}", candidates);
            return null;
        }
        for (Candidate candidate : candidates) {
            CandidateResponse response = new CandidateResponse();
            response.setEmail(candidate.getEmail());
            response.setId(candidate.getId());
            response.setMobileNumber(candidate.getMobileNumber());
            response.setName(candidate.getName());
            response.setProfileImageId(candidate.getProfileImageId());
            response.setResumeId(candidate.getResumeId());
            responses.add(response);
        }
        return responses;
    }

    public CandidateDetails getCandidateDetailsById(UserDTO user, String id) {
        log.info("Get details for the user :{}", user);
        Candidate candidate = candidateService.getCandidateById(id);
        if (ObjectUtils.isEmpty(candidate)) {
            log.info("Candidate not found for the id :{}", id);
            throw new BadException("Candidate not found for the id " + id);
        }
        CandidateDetails details = new CandidateDetails();
        details.setId(candidate.getId());
        details.setName(candidate.getName());
        details.setEmail(candidate.getEmail());
        details.setMobileNumber(candidate.getMobileNumber());
        details.setAge(candidate.getAge());
        details.setGender(candidate.getGender());
        details.setSkills(candidate.getSkills());
        details.setHighestQualification(candidate.getHighestQualification());
        details.setProfileImageId(candidate.getProfileImageId());
        details.setResumeId(candidate.getResumeId());
        details.setLocation(candidate.getLocation().getCity() + ", " + candidate.getLocation().getState() + ", "
                + candidate.getLocation().getCountry());
        return details;
    }

}
