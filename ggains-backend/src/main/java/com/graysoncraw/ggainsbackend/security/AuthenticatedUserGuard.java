package com.graysoncraw.ggainsbackend.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserGuard {

    public String getAuthenticatedFirebaseUid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            throw new AccessDeniedException("Authenticated principal is missing");
        }

        if (principal instanceof String uid && !uid.isBlank()) {
            return uid;
        }

        String name = authentication.getName();
        if (name == null || name.isBlank()) {
            throw new AccessDeniedException("Authenticated principal is missing");
        }

        return name;
    }

    public void requireUidMatches(String requestedFirebaseUid) {
        String authenticatedUid = getAuthenticatedFirebaseUid();
        if (!authenticatedUid.equals(requestedFirebaseUid)) {
            throw new AccessDeniedException("You are not allowed to access this user's data");
        }
    }
}
