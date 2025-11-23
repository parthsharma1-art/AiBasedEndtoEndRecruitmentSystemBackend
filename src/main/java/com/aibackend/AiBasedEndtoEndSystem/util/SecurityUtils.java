package com.aibackend.AiBasedEndtoEndSystem.util;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public class SecurityUtils {
    public static UserDTO getLoggedInUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        if (auth.getPrincipal() instanceof UserDTO userDTO) {
            return userDTO;
        }
        return null;
    }

    public static ObjectId getLoggedInUserId() {
        UserDTO user = getLoggedInUser();
        return user != null ? user.getId() : null;
    }

    public static String getLoggedInUserRole() {
        UserDTO user = getLoggedInUser();
        return user != null ? user.getRole().name() : null;
    }
}
