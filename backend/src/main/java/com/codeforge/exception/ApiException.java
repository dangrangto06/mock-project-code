package com.codeforge.exception;

/**
 * Domain-specific runtime exception representing business logic errors across the application.
 *
 * <p>Thrown by service components (such as {@link com.codeforge.service.AuthService} or
 * {@link com.codeforge.service.ExerciseService}) to abort execution and return a controlled
 * HTTP error status. Intercepted centrally by {@link GlobalExceptionHandler} to generate
 * standardized JSON error payloads for client consumption.</p>
 *
 * <p>Includes convenience static factory methods for common HTTP error conditions (400, 401, 409).</p>
 */
public class ApiException extends RuntimeException {

    /**
     * The targeted HTTP status code associated with this error (e.g., 400, 401, 409).
     */
    private final int httpStatus;

    /**
     * Constructs a new {@link ApiException} with a designated HTTP status and descriptive message.
     *
     * @param httpStatus The HTTP status code to transmit in the response.
     * @param message    Human-readable description explaining the failure reason.
     */
    public ApiException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    /**
     * Gets the associated HTTP status code.
     *
     * @return The HTTP status code.
     */
    public int getHttpStatus() {
        return httpStatus;
    }

    // --- Convenience factory methods ---

    /**
     * Factory method creating an HTTP 401 Unauthorized exception.
     *
     * @param message The unauthorized error message.
     * @return New {@link ApiException} instance with status 401.
     */
    public static ApiException unauthorized(String message) {
        return new ApiException(401, message);
    }

    /**
     * Factory method creating an HTTP 409 Conflict exception.
     *
     * @param message The conflict error message (e.g. duplicate username or email).
     * @return New {@link ApiException} instance with status 409.
     */
    public static ApiException conflict(String message) {
        return new ApiException(409, message);
    }

    /**
     * Factory method creating an HTTP 400 Bad Request exception.
     *
     * @param message The bad request error message.
     * @return New {@link ApiException} instance with status 400.
     */
    public static ApiException badRequest(String message) {
        return new ApiException(400, message);
    }
}
