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
        private long totalFeedbacks;
        private long pendingProblemsCount;
        private long pendingDiscussionsCount;
        private long pendingReportsCount;
        private long pendingFeedbacksCount;
    }

    /**
     * Unified moderation queue holding unreviewed problem challenges, community discussion threads,
     * and user-reported discussions/replies.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ModerationQueueResponse {
        private List<PendingProblemDto> pendingProblems;
        private List<PendingDiscussionDto> pendingDiscussions;
        private List<PendingReportDto> pendingReports;
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
    public static class PendingReportDto {
        private Long id;
        private String reportType;
        private Long targetId;
        private String targetTitle;
        private String targetContent;
        private String targetAuthorName;
        private String reporterName;
        private String reporterEmail;
        private String reason;
        private String details;
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

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TrackingSummaryDto {
        private long totalLogins;
        private long uniqueLocations;
        private long totalPageVisits;
        private String mostVisitedPage;
        private List<TopPageDto> topPages;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopPageDto {
        private String pageRoute;
        private String pageTitle;
        private long visits;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserLoginLogDto {
        private Long id;
        private Long userId;
        private String username;
        private String email;
        private String loginMethod;
        private String ipAddress;
        private String location;
        private String userAgent;
        private LocalDateTime loggedInAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PageVisitLogDto {
        private Long id;
        private Long userId;
        private String username;
        private String pageRoute;
        private String pageTitle;
        private String ipAddress;
        private String location;
        private LocalDateTime visitedAt;
    }
}

