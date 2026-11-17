package com.codeforge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Objects for Administrator and Moderation endpoints.
 */
public class AdminDto {

    /**
     * High-level platform telemetry statistics for the admin overview dashboard.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatsResponse {
        private long totalUsers;
        private long totalProblems;
        private long totalDiscussions;
        private long totalSubmissions;
        private long pendingProblemsCount;
        private long pendingDiscussionsCount;
    }

    /**
     * Unified moderation queue holding unreviewed problem challenges and community discussion threads.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModerationQueueResponse {
        private List<PendingProblemDto> pendingProblems;
        private List<PendingDiscussionDto> pendingDiscussions;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PendingProblemDto {
        private Long id;
        private String title;
        private String difficulty;
        private List<String> topics;
        private String description;
        private String authorName;
        private String authorEmail;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PendingDiscussionDto {
        private Long id;
        private String title;
        private String category;
        private Long problemId;
        private String problemTitle;
        private String content;
        private String authorName;
        private String authorEmail;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RejectRequest {
        @NotBlank(message = "Rejection reason is required")
        private String reason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRoleRequest {
        @NotBlank(message = "Role is required")
        private String role;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminUserDto {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String role;
        private Integer solvedCount;
        private LocalDateTime createdAt;
    }
}

