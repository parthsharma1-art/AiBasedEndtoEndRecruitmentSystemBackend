package com.aibackend.AiBasedEndtoEndSystem.util;

import com.aibackend.AiBasedEndtoEndSystem.dto.UserDTO;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.security.AppPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public final class SecurityUtils {

    private SecurityUtils() {}

    public static AppPrincipal getLoggedInPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.info("SecurityUtils - auth: {}, principal class: {}",
                auth, auth == null ? "null" : auth.getPrincipal().getClass().getName());
        if (auth == null) {
            throw new BadException("No authentication in security context");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof AppPrincipal) {
            return (AppPrincipal) principal;
        }
        throw new BadException("Authenticated principal is not an AppPrincipal");
    }

    public static String getLoggedInUserTypeAsString() {
        return getLoggedInPrincipal().getUserType().toString();
    }

    public static UserDTO.UserType getLoggedInUserType() {
        String type = getLoggedInUserTypeAsString();
        if (type == null) return null;
        try {
            return UserDTO.UserType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static String getLoggedInUserIdAsString() {
        return getLoggedInPrincipal().getId().toString();
    }

    public static ObjectId getLoggedInUserIdAsObjectId() {
        String id = getLoggedInUserIdAsString();
        if (id == null || id.isBlank()) {
            throw new BadException("Logged-in user id is null or blank");
        }
        try {
            return new ObjectId(id);
        } catch (IllegalArgumentException ex) {
            throw new BadException("Logged-in user id is not a valid ObjectId: " + id);
        }
    }
}
