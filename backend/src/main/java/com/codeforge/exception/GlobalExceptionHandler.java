package com.codeforge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralized exception handling interceptor for all REST controllers.
 *
 * <p>Annotated with {@link RestControllerAdvice} to catch exceptions thrown during request processing
 * and translate them into consistent, standardized JSON error responses with proper HTTP status codes.</p>
 *
 * <p><strong>Handled Exception Types:</strong></p>
 * <ul>
 *   <li>{@link ApiException}: Custom domain errors specifying custom HTTP status codes (e.g., 401, 409).</li>
 *   <li>{@link MethodArgumentNotValidException}: Triggered by Jakarta Bean Validation ({@code @Valid}) failures on DTOs.</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Intercepts and handles application-level business exceptions ({@link ApiException}).
     *
     * <p>Maps the exception directly to its encapsulated HTTP status code and returns a JSON
     * payload containing the error {@code message}.</p>
     *
     * @param ex The intercepted {@link ApiException}.
     * @return {@link ResponseEntity} containing a map with the error message and the target HTTP status.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getHttpStatus())
                .body(Map.of("message", ex.getMessage()));
    }

    /**
     * Intercepts and formats bean validation errors triggered by {@code @Valid} annotations on request bodies.
     *
     * <p>Extracts all field validation failures, concatenates them into a semicolon-delimited string,
     * and returns an HTTP 400 Bad Request response.</p>
     *
     * @param ex The validation exception containing binding and field error details.
     * @return {@link ResponseEntity} with HTTP 400 status and the compiled error messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", message));
    }
}
