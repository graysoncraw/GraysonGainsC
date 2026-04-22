package com.graysoncraw.ggainsbackend.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticatedUserGuardTest {

    private final AuthenticatedUserGuard guard = new AuthenticatedUserGuard();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsAuthenticatedUidFromSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("uid-123", null, List.of())
        );

        String uid = guard.getAuthenticatedFirebaseUid();
        assertEquals("uid-123", uid);
    }

    @Test
    void throwsAccessDeniedWhenRequestedUidDoesNotMatchAuthenticatedUid() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("uid-123", null, List.of())
        );

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> guard.requireUidMatches("uid-456")
        );

        assertEquals("You are not allowed to access this user's data", exception.getMessage());
    }

    @Test
    void throwsAccessDeniedWhenNoAuthenticationExists() {
        SecurityContextHolder.clearContext();

        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                guard::getAuthenticatedFirebaseUid
        );

        assertEquals("User is not authenticated", exception.getMessage());
    }
}
