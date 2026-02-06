package com.aibackend.AiBasedEndtoEndSystem.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "candidates")
public class Candidate {

    @Id
    private String id;

    private String userId;                 // link to User collection
    private String name;
    private String email;
    private String mobileNumber;
    private Integer age;
    private String gender;

    private Location location;

    private List<String> skills;
    private Integer experienceYears;
    private String highestQualification;
    private String currentJobRole;
    private String currentCompany;

    private Integer expectedSalary;
    private Integer currentSalary;

    private String resumeUrl;
    private String profileImageUrl;

    private String aiOverallScore;          // overall score from resume parsing

    @Data
    public static class Location {
        private String city;
        private String state;
        private String country;
    }
}
