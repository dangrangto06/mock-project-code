package com.codeforge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Data Transfer Objects for Learner Notes operations.
 */
public class NoteDto {

    /**
     * Response payload representing a learner note.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private String title;
        private Long problemId;
        private String problemTitle;
        private List<String> tags;
        private String content;
        private LocalDateTime updatedAt;
    }

    /**
     * Inbound payload for creating or updating a learner note.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        @NotBlank(message = "Title is required")
        private String title;

        private Long problemId;

        private String problemTitle;

        private List<String> tags;

        private String content;
    }

    /**
     * Helper to parse comma-separated tags string into a list of strings.
     */
    public static List<String> parseTags(String tagsString) {
        if (tagsString == null || tagsString.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tagsString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Helper to serialize list of tags into a comma-separated string.
     */
    public static String serializeTags(List<String> tagsList) {
        if (tagsList == null || tagsList.isEmpty()) {
            return "";
        }
        return tagsList.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
    }
}

