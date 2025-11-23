package com.aibackend.AiBasedEndtoEndSystem.dto;


import lombok.Data;

import java.util.List;

@Data
public class CandidateRequest {

    private String name;
    private String email;
    private String mobileNumber;
    private Integer age;
    private String gender;
    private LocationDTO location;
    private List<String> skills;
    private Integer experienceYears;
    private String highestQualification;
    private String currentJobRole;
    private String resumeUrl;
    private String profileImageUrl;
    private Integer expectedSalary;
    private String cityPreference;

    @Data
    public static class LocationDTO {
        private String city;
        private String state;
        private String country;
    }
}

