package com.graysoncraw.ggainsbackend.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsIllegalArgumentToBadRequest() {
        HttpServletRequest request = mockRequest("/api/test");
        ResponseEntity<ApiErrorResponse> response = handler.handleBadRequest(new IllegalArgumentException("Bad input"), request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Bad input", response.getBody().message());
        assertEquals("/api/test", response.getBody().path());
    }

    @Test
    void mapsIllegalStateToConflict() {
        HttpServletRequest request = mockRequest("/api/test");
        ResponseEntity<ApiErrorResponse> response = handler.handleConflict(new IllegalStateException("Conflict"), request);

        assertEquals(409, response.getStatusCode().value());
        assertEquals("Conflict", response.getBody().message());
    }

    @Test
    void mapsAccessDeniedToForbidden() {
        HttpServletRequest request = mockRequest("/api/test");
        ResponseEntity<ApiErrorResponse> response = handler.handleForbidden(new AccessDeniedException("Forbidden"), request);

        assertEquals(403, response.getStatusCode().value());
        assertEquals("Forbidden", response.getBody().message());
    }

    @Test
    void mapsNotFoundExceptionsToNotFound() {
        HttpServletRequest request = mockRequest("/api/test");
        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(new NoSuchElementException("Missing"), request);
        ResponseEntity<ApiErrorResponse> response2 = handler.handleNotFound(new EntityNotFoundException("Entity missing"), request);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Missing", response.getBody().message());
        assertEquals(404, response2.getStatusCode().value());
    }

    @Test
    void mapsUnexpectedToInternalServerError() {
        HttpServletRequest request = mockRequest("/api/test");
        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(new RuntimeException("Boom"), request);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Unexpected server error", response.getBody().message());
    }

    @Test
    void mapsValidationToBadRequestWithFieldMessage() throws Exception {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "target");
        bindingResult.addError(new FieldError("target", "email", "must not be blank"));

        Method method = ValidationTarget.class.getDeclaredMethod("accept", String.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);
        HttpServletRequest request = mockRequest("/api/users");

        ResponseEntity<ApiErrorResponse> response = handler.handleValidation(exception, request);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("email: must not be blank", response.getBody().message());
    }

    private HttpServletRequest mockRequest(String path) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(path);
        return request;
    }

    private static class ValidationTarget {
        @SuppressWarnings("unused")
        void accept(String value) {
        }
    }
}
