package com.backend.sensor_data.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleDataIntegrityViolation_returns409WithDetails() {
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("duplicate key", new RuntimeException("root cause"));

        ResponseEntity<Map<String, String>> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Database constraint violation.", response.getBody().get("error"));
    }

    @Test
    void handleIllegalArgumentException_returns400WithMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("bad input");

        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("bad input", response.getBody().get("details"));
    }

    @Test
    void handleMethodNotSupported_returns405() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("PATCH");

        ResponseEntity<Map<String, String>> response = handler.handleMethodNotSupported(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
    }

    @Test
    void handleGenericException_returns500() {
        Exception ex = new RuntimeException("unexpected failure");

        ResponseEntity<Map<String, String>> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("unexpected failure", response.getBody().get("details"));
    }
}