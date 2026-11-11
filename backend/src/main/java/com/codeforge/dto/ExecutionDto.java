package com.codeforge.dto;

import lombok.*;

import java.util.List;

/**
 * Data Transfer Objects (DTOs) for code execution requests and evaluation responses.
 *
 * <p>Defines client-server contracts for sandbox dry-runs (testing against custom/sample inputs)
 * and formal submissions (evaluating against full test suites and recording submission entities).</p>
 */
public class ExecutionDto {

    /**
     * Inbound payload for sandbox test execution ({@code POST /api/execute/run}).
     * Does not trigger database persistence of submission records.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RunRequest {

        /**
         * Optional problem ID context for this run.
         */
        private Long problemId;

        /**
         * Programming language identifier (e.g., 'python', 'javascript', 'cpp', 'java').
         */
        private String language;

        /**
         * Source code string to be executed in the sandbox.
         */
        private String code;

        /**
         * Optional custom standard input supplied by the user.
         */
        private String customInput;
    }

    /**
     * Outbound response returned after executing a sandbox run.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RunResponse {

        /**
         * Boolean indicating whether execution succeeded without errors.
         */
        private Boolean success;

        /**
         * Execution status description (e.g., "Accepted", "Wrong Answer", "Compilation Error").
         */
        private String status;

        /**
         * Count of sample test cases that passed.
         */
        private Integer passedCount;

        /**
         * Total number of sample test cases evaluated.
         */
        private Integer totalCount;

        /**
         * Execution duration string (e.g., "0.032s" or "32 ms").
         */
        private String runtime;

        /**
         * Memory consumed string (e.g., "17.4 MB").
         */
        private String memory;

        /**
         * Detailed results per test case evaluated.
         */
        private List<ExerciseDto.TestCaseDto> results;
    }

    /**
     * Inbound payload for submitting a solution for formal evaluation ({@code POST /api/execute/submit}).
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmitRequest {

        /**
         * Target exercise ID being solved.
         */
        private Long problemId;

        /**
         * Programming language identifier.
         */
        private String language;

        /**
         * Complete source code solution to be judged.
         */
        private String code;
    }

    /**
     * Outbound response returned after judging a formal submission against all test cases.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmitResponse {

        /**
         * Indicates whether every test case passed and the submission is accepted.
         */
        private Boolean success;

        /**
         * Overall submission status verdict (e.g., "Accepted", "Wrong Answer").
         */
        private String status;

        /**
         * Number of test cases that produced expected output.
         */
        private Integer testsPassed;

        /**
         * Total number of judging test cases evaluated.
         */
        private Integer totalTests;

        /**
         * Average execution time formatted with unit (e.g., "32 ms").
         */
        private String runtime;

        /**
         * Percentile comparison of runtime speed against other submissions (e.g., "84.5%").
         */
        private String runtimeBeats;

        /**
         * Peak memory utilization formatted with unit (e.g., "17.4 MB").
         */
        private String memory;

        /**
         * Percentile comparison of memory efficiency against other submissions (e.g., "78.2%").
         */
        private String memoryBeats;

        /**
         * Congratulatory or diagnostic message explaining test results.
         */
        private String message;
    }
}
