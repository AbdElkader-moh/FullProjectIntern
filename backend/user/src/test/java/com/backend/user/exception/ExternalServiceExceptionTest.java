package com.backend.user.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalServiceExceptionTest {

    @Test
    void constructor_setsMessage() {
        ExternalServiceException ex = new ExternalServiceException("Cloudinary upload failed");

        assertEquals("Cloudinary upload failed", ex.getMessage());
    }

    @Test
    void isARuntimeException() {
        ExternalServiceException ex = new ExternalServiceException("some failure");

        assertTrue(ex instanceof RuntimeException);
    }
}
