package com.aibackend.AiBasedEndtoEndSystem.entity;

import com.aibackend.AiBasedEndtoEndSystem.security.AppPrincipal;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Data
@Document(collection = "recruiters")
public class Recruiter {

    @Id
    private ObjectId id;

    private String userId;            // link to User collection
    private String name;
    private String email;
    private String mobileNumber;

    private String companyName;
    private String companyId;
    private String designation;

    private Boolean active = true;
}

