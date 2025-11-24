package com.aibackend.AiBasedEndtoEndSystem.entity;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String name;
    private Integer age;
    private String email;
    private String mobileNumber;
    private String state;
    private String type;

    public enum Role{
        RECRUITER,CANDIDATE,USER
    }
}
