package com.aibackend.AiBasedEndtoEndSystem.security;

import org.bson.types.ObjectId;

public interface AppPrincipal {
    ObjectId getId();
    String getUsername();
    String getUserEmail();// e.g. "hr" or "candidate"
}
