package com.backend.user.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers every @ExceptionHandler method in GlobalExceptionHandler:
 * status code + body shape ({"message": ...}) for each mapped exception type.
 *
 * NOTE: assumes ConflictException / UnauthorizedException / NotFoundException
 * follow the same single-String-message constructor pattern already
 * confirmed for ExternalServiceException in this codebase. If any of those
 * three have a different constructor signature, adjust the corresponding
 * test's instantiation line only — nothing else changes.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleConflict_returns409WithMessage() {
        ConflictException ex = new ConflictException("Email already registered");

        ResponseEntity<Map<String, String>> response = handler.handleConflict(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Email already registered", response.getBody().get("message"));
    }

    @Test
    void handleUnauthorized_returns401WithMessage() {
        UnauthorizedException ex = new UnauthorizedException("Not authenticated");

        ResponseEntity<Map<String, String>> response = handler.handleUnauthorized(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Not authenticated", response.getBody().get("message"));
    }

    @Test
    void handleNotFound_returns404WithMessage() {
        NotFoundException ex = new NotFoundException("User not found.");

        ResponseEntity<Map<String, String>> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("User not found.", response.getBody().get("message"));
    }

    @Test
    void handleValidation_returns400WithFixedMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.<FieldError>of());

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("All Fields Must be filled", response.getBody().get("message"));
    }

    @Test
    void handleExternalService_returns502WithMessage() {
        ExternalServiceException ex = new ExternalServiceException("Cloudinary upload failed");

        ResponseEntity<Map<String, String>> response = handler.handleExternalService(ex);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("Cloudinary upload failed", response.getBody().get("message"));
    }

    @Test
    void handleIllegalArgument_returns400WithMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("Threshold for co must be between 0 and 50.");

        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Threshold for co must be between 0 and 50.", response.getBody().get("message"));
    }

    @Test
    void handleGeneric_returns500WithGenericMessage_doesNotLeakExceptionDetail() {
        RuntimeException ex = new RuntimeException("some internal stack trace detail that should not leak");

        ResponseEntity<Map<String, String>> response = handler.handleGeneric(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred.", response.getBody().get("message"));
    }
}
