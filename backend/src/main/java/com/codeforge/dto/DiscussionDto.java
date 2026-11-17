package com.codeforge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Objects for Community Discussion topics and replies.
 */
public class DiscussionDto {

    /**
     * Response payload representing a discussion thread matching frontend structure.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private String authorName;
        private String authorInitials;
        private String authorRole;
        private String category;
        private Long problemId;
        private String problemTitle;
        private String content;
        private int upvotes;
        private Boolean isUpvoted;
        private int repliesCount;
        private String createdAt;
        private String moderationStatus;
        private String rejectionReason;
        @Builder.Default
        private List<ReplyResponse> replies = new ArrayList<>();
    }

    /**
     * Response payload representing an individual reply.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReplyResponse {
        private Long id;
        private String authorName;
        private String authorInitials;
        private String authorRole;
        private String content;
        private String createdAt;
    }

    /**
     * Inbound payload for creating a new discussion topic.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateDiscussionRequest {
        @NotBlank(message = "Title is required")
        private String title;

        @NotBlank(message = "Category is required")
        private String category;

        private Long problemId;

        private String problemTitle;

        @NotBlank(message = "Content is required")
        private String content;
    }

    /**
     * Inbound payload for submitting a reply to an existing discussion.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateReplyRequest {
        @NotBlank(message = "Content is required")
        private String content;
    }

    public static String getInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "U";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length > 1) {
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }
        return parts[0].substring(0, 1).toUpperCase();
    }

    public static String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "Recently";
        return dateTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }
}

