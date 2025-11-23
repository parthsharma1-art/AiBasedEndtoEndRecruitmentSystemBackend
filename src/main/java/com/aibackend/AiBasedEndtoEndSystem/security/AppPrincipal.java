package com.aibackend.AiBasedEndtoEndSystem.security;

import com.aibackend.AiBasedEndtoEndSystem.entity.User;
import org.bson.types.ObjectId;

public interface AppPrincipal {
    ObjectId getId();
    String getUsername();
    String getUserEmail();// e.g. "hr" or "candidate"
    User.Role getRole();
}
