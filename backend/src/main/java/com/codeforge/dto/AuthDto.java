package com.codeforge.dto;

import jakarta.validation.constraints.NotBlank;
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
        private String fullName;

        /**
         * Desired unique account handle.
         */
        @NotBlank(message = "Username is required")
        private String username;

        /**
         * Unique contact email address.
         */
        @NotBlank(message = "Email is required")
        private String email;

        /**
         * Desired plaintext password to be salted and hashed via BCrypt.
         */
        @NotBlank(message = "Password is required")
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
}
