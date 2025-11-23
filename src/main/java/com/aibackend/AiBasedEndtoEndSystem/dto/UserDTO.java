package com.aibackend.AiBasedEndtoEndSystem.dto;

import com.aibackend.AiBasedEndtoEndSystem.entity.User;
import com.aibackend.AiBasedEndtoEndSystem.security.AppPrincipal;
import lombok.Data;
import org.bson.types.ObjectId;

@Data
public class UserDTO implements AppPrincipal {
    private ObjectId id;
    private String username;
    private String userEmail;
    private User.Role role;

    public enum UserType{
        RECRUITER,CANDIDATE,USER
    }

    @Override
    public ObjectId getId() {
        return id == null ? null : id;
    }
    @Override
    public String getUserEmail(){
        return userEmail==null?null:userEmail.toString();
    }

    @Override
    public String getUsername() {
        return username;
    }

}

