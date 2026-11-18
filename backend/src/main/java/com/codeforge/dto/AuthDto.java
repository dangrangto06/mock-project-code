package com.codeforge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Data Transfer Objects (DTOs) encapsulating user authentication requests and responses.
 *
 * <p>Groups data contracts for credentials validation, user registration payloads,
 * and authenticated session responses returned upon successful verification.</p>
 */
public class AuthDto {

    /**
     * Inbound payload for the user login endpoint ({@code POST /api/auth/login}).
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginRequest {

        /**
         * The user's handle or registered email address.
         */
        @NotBlank(message = "Username or email is required")
        private String usernameOrEmail;

        /**
         * Plaintext password submitted for authentication against the stored BCrypt hash.
         */
        @NotBlank(message = "Password is required")
        private String password;
    }

    /**
     * Inbound payload for user account registration ({@code POST /api/auth/register}).
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegisterRequest {

        /**
         * Full display name of the applicant.
         */
        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must be at most 150 characters")
        private String fullName;

        /**
         * Desired unique account handle: 3-20 letters, digits or underscores
         * (same rule as USERNAME_PATTERN in frontend/js/signup.js).
         */
        @NotBlank(message = "Username is required")
        @Pattern(regexp = "^[A-Za-z0-9_]{3,20}$",
                message = "Username must be 3-20 characters: letters, digits or underscore")
        private String username;

        /**
         * Unique contact email address. The extra regexp mirrors EMAIL_PATTERN in
         * frontend/js/signup.js (requires a dot in the domain, which a bare
         * {@code @Email} does not); 255 matches the users.email column length.
         */
        @NotBlank(message = "Email is required")
        @Email(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must be at most 255 characters")
        private String email;

        /**
         * Desired plaintext password to be salted and hashed via BCrypt.
         * Minimum 8 (MIN_PASSWORD_LENGTH in signup.js); maximum 72 because BCrypt
         * ignores anything beyond 72 bytes.
         */
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        private String password;
    }

    /**
     * Outbound payload returned upon successful authentication or registration.
     * Contains the issued JWT access token and user identity profile.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthResponse {

        /**
         * Cryptographically signed JWT access token string.
         */
        private String token;

        /**
         * Authorization scheme type (defaults to "Bearer").
         */
        @Builder.Default
        private String type = "Bearer";

        /**
         * Primary key ID of the authenticated user.
         */
        private Long id;

        /**
         * Username handle of the authenticated user.
         */
        private String username;

        /**
         * Email address of the authenticated user.
         */
        private String email;

        /**
         * Full name of the authenticated user.
         */
        private String fullName;

        /**
         * Assigned authorization role (e.g., 'Learner', 'Admin').
         */
        private String role;
    }

    /**
     * Inbound payload for requesting a password reset email ({@code POST /api/auth/forgot-password}).
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        private String email;
    }

    /**
     * Inbound payload for setting a new password using a reset token ({@code POST /api/auth/reset-password}).
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResetPasswordRequest {
        @NotBlank(message = "Token is required")
        private String token;

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        private String newPassword;
    }

    /**
     * Simple outbound message response payload.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MessageResponse {
        private String message;
    }
}
