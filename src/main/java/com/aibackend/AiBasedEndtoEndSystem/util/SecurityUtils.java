package com.aibackend.AiBasedEndtoEndSystem.util;

import com.aibackend.AiBasedEndtoEndSystem.entity.User;
import com.aibackend.AiBasedEndtoEndSystem.exception.BadException;
import com.aibackend.AiBasedEndtoEndSystem.security.AppPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AppPrincipal getLoggedInPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new BadException("No authentication in security context");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof AppPrincipal) {
            log.info("The principal instance is :{}", (AppPrincipal) principal);
            return (AppPrincipal) principal;
        }
        throw new BadException("Authenticated principal is not an AppPrincipal");
    }

    public static User.Role getLoggedInUserRole() {
        return getLoggedInPrincipal().getRole();   // returns HR / CANDIDATE / USER
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
