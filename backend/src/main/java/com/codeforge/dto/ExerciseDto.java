package com.codeforge.dto;

import lombok.*;

import java.util.List;

/**
 * Data Transfer Object (DTO) representing an exercise across problem lists and IDE views.
 *
 * <p>Translates JPA {@link com.codeforge.entity.Exercise} entities into client-optimized view models,
 * including user-contextual flags such as whether the problem has been solved, bookmarked, or authored
 * by the current user.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseDto {

    /**
     * Unique problem identifier (e.g., 104).
     */
    private Long id;

    /**
     * Problem title (e.g., "Pair Sum").
     */
    private String title;

    /**
     * Difficulty level ("Easy", "Medium", "Hard").
     */
    private String difficulty;

    /**
     * Categorization tags and topic list (e.g., ["Arrays", "Hash map"]).
     */
    private List<String> topics;

    /**
     * Formatted acceptance rate string (e.g., "78.4%").
     */
    private String acceptance;

    /**
     * Progress status for the calling user: 'solved', 'in-progress', or 'unattempted'.
     */
    private String status;

    /**
     * Flag indicating whether the problem was bookmarked/saved by the calling user.
     */
    private Boolean saved;

    /**
     * Flag indicating whether the problem was authored by the calling user.
     */
    private Boolean isMine;

    /**
     * Display name of the problem author or "CodeForge".
     */
    private String author;

    /**
     * Last update date formatted for presentation.
     */
    private String updatedDate;

    /**
     * Full problem description statement in Markdown.
     */
    private String description;

    /**
     * Problem constraints in Markdown.
     */
    private String constraints;

    /**
     * Default starter code snippet for the active language.
     */
    private String starterCode;

    /**
     * List of sample test cases rendered in the IDE.
     */
    private List<TestCaseDto> testCases;

    /**
     * DTO representing an individual test case result or sample input.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestCaseDto {

        /**
         * Test case identifier.
         */
        private Long id;

        /**
         * Test case display name (e.g., "Case 1").
         */
        private String name;

        /**
         * Input string fed into stdin or function argument.
         */
        private String input;

        /**
         * Expected output string.
         */
        private String expected;

        /**
         * Actual output produced by the solution code.
         */
        private String yourOutput;

        /**
         * Verdict flag: {@code true} if actual matches expected, {@code false} otherwise.
         */
        private Boolean passed;
    }

    /**
     * Inbound payload for creating a custom coding problem ({@code POST /api/problems}).
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateExerciseRequest {

        /**
         * Problem title.
         */
        private String title;

        /**
         * Markdown statement describing the problem.
         */
        private String statement;

        /**
         * Target difficulty level ("Easy", "Medium", "Hard").
         */
        private String difficulty;

        /**
         * Category topic tags.
         */
        private List<String> topics;

        /**
         * Default programming language for initial starter template.
         */
        private String language;

        /**
         * Configured time limit (e.g., "1,000 ms").
         */
        private String timeLimit;

        /**
         * Configured memory limit (e.g., "256 MB").
         */
        private String memoryLimit;
    }
}
