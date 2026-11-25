package com.codeforge.dto;

import lombok.*;

import java.util.List;

/**
 * Data Transfer Objects (DTOs) for code compilation, execution requests, and evaluation responses.
 *
 * <p>Defines client-server contracts for sandbox dry-runs (testing against custom/sample inputs),
 * standalone Judge0 compiler execution, language discovery, and formal judged submissions.</p>
 */
public class ExecutionDto {

    /**
     * Custom test case input/expected pair supplied by the IDE client.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomTestCaseInput {
        private String name;
        private String input;
        private String expected;
    }

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

        /**
         * Optional list of custom/edited sample test cases from the IDE console.
         */
        private List<CustomTestCaseInput> customCases;
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
         * Execution duration string (e.g., "32 ms").
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

        /**
         * Optional compiler diagnostics produced during compilation (Status ID 6).
         */
        private String compileOutput;

        /**
         * Execution engine used ("Judge0 CE", "Judge0 RapidAPI", "Judge0 Self-Hosted", or "Local Sandbox Fallback").
         */
        private String engine;
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

        /**
         * Optional compiler diagnostics if submission failed to compile.
         */
        private String compileOutput;

        /**
         * Execution engine used.
         */
        private String engine;
    }

    /**
     * Inbound payload for standalone Judge0 compiler execution ({@code POST /api/execute/compile}).
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompileRequest {

        /**
         * Canonical Judge0 numeric language ID (e.g. 100 for Python 3.12, 105 for C++ GCC 14).
         */
        private Integer languageId;

        /**
         * Optional string alias fallback (e.g. "python", "cpp", "java", "javascript").
         */
        private String language;

        /**
         * Complete program source code to compile and execute.
         */
        private String sourceCode;

        /**
         * Standard input passed to the program.
         */
        private String stdin;

        /**
         * Optional expected standard output for automated comparison.
         */
        private String expectedOutput;

        /**
         * Optional CPU time limit in seconds (0.1 .. 15.0).
         */
        private Double cpuTimeLimit;

        /**
         * Optional memory limit in kilobytes (2048 .. 512000).
         */
        private Integer memoryLimit;
    }

    /**
     * Outbound telemetry payload returned by the Judge0 Compiler endpoint.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompileResponse {
        private Boolean success;
        private Integer statusId;
        private String status;
        private String token;
        private Integer languageId;
        private String languageName;
        private String stdout;
        private String stderr;
        private String compileOutput;
        private String message;
        private Integer exitCode;
        private String time;
        private String runtime;
        private Integer memoryKb;
        private String memory;
        private String engine;
    }

    /**
     * Supported compiler language metadata from Judge0.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Judge0LanguageDto {
        private Integer id;
        private String name;
        private String key;
        private Boolean isArchived;
    }
}
