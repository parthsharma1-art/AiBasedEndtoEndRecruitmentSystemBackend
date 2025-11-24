package com.aibackend.AiBasedEndtoEndSystem.dto;

import com.aibackend.AiBasedEndtoEndSystem.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bson.types.ObjectId;

@Data
public class UserDTO  {
    private String id;
    private String username;
    private String userEmail;
    private String mobileNumber;
    private String role;
}

