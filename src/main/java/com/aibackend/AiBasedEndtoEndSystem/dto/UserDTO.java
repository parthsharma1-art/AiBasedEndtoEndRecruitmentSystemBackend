package com.aibackend.AiBasedEndtoEndSystem.dto;

import com.aibackend.AiBasedEndtoEndSystem.entity.User;
import lombok.Data;
import org.bson.types.ObjectId;

@Data
public class UserDTO  {
    private ObjectId id;
    private String username;
    private String userEmail;
    private User.Role role;

}

