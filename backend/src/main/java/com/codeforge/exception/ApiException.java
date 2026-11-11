package com.codeforge.exception;

/**
 * Business-logic exception thrown by service classes (e.g. AuthService).
 * Caught by GlobalExceptionHandler and mapped to an appropriate HTTP status.
 */
public class ApiException extends RuntimeException {

    private final int httpStatus;

    public ApiException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    // --- Convenience factory methods ---

    public static ApiException unauthorized(String message) {
        return new ApiException(401, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(409, message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(400, message);
    }
}

