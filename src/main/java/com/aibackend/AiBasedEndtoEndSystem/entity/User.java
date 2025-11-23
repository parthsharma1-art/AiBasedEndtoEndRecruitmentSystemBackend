package com.aibackend.AiBasedEndtoEndSystem.entity;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.security.AppPrincipal;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "users")
public class User implements AppPrincipal {
    @Id
    private ObjectId id;
    private String name;
    private Integer age;
    private Role role;
    private String mobileNumber;
    private String state;
    private String type;

    @Override
    public String getUsername() {
        return "";
    }

    @Override
    public String getUserEmail() {
        return "";
    }

    public enum Role{
        RECRUITER,CANDIDATE,USER
    }
}
