
package com.backend.user.exception;

/**
 * Thrown when a call to an external/upstream service (e.g. Cloudinary) fails
 * or is unavailable. Distinct from a generic RuntimeException so callers and
 * the global exception handler can map it to a specific HTTP status (502)
 * instead of a bare 500.
 */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }
}