package com.aibackend.AiBasedEndtoEndSystem.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Data
@Document(collection = "recruiters")
public class Recruiter {

    @Id
    private String id;
    private String userId;            // link to User collection
    private String name;
    private String email;
    private String mobileNumber;

    private String companyName;
    private String companyId;
    private String designation;

    // File references
    private String profileImageId;
    private String idCardFileId;

    private Boolean active = true;
}

