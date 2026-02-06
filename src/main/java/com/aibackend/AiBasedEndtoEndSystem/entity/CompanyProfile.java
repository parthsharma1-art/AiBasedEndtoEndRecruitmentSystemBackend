package com.aibackend.AiBasedEndtoEndSystem.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "company_profile")
public class CompanyProfile {
    @Id
    private String id;
    private String recruiterId;
    private BasicSetting basicSetting;
    private ContactDetails contactDetails;
    private SocialLinks socialLinks;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;


    @Data
    public static class BasicSetting {
        private String companyName;
        private String companyDomain;

    }

    @Data
    public static class ContactDetails {
        private String companyEmail;
        private String companyMobileNumber;
        private String companyAddress;

    }

    @Data
    public static class SocialLinks {
        private String facebook;
        private String instagram;
        private String google;
        private String twitter;

    }
}
