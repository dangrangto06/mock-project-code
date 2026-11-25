package com.codeforge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Data Transfer Objects for the User Feedback feature.
 */
public class FeedbackDto {

    /**
     * Submission payload submitted from the frontend feedback form.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        private String email;

        @Size(max = 200, message = "Subject must not exceed 200 characters")
        private String subject;

        @NotBlank(message = "Feedback message cannot be empty")
        @Size(max = 4000, message = "Message must not exceed 4000 characters")
        private String message;
    }

    /**
     * Response payload returned to client or presented in the admin console.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long userId;
        private String username;
        private String name;
        private String email;
        private String subject;
        private String message;
        private String status;
        private LocalDateTime createdAt;
    }

    /**
     * Status update payload for administrators.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        @NotBlank(message = "Status cannot be blank")
        private String status;
    }
}
