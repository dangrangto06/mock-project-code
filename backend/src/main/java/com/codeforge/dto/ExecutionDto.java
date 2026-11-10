package com.codeforge.dto;

import lombok.*;

import java.util.List;

public class ExecutionDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RunRequest {
        private Long problemId;
        private String language;
        private String code;
        private String customInput;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RunResponse {
        private Boolean success;
        private String status;
        private Integer passedCount;
        private Integer totalCount;
        private String runtime;
        private String memory;
        private List<ExerciseDto.TestCaseDto> results;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmitRequest {
        private Long problemId;
        private String language;
        private String code;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmitResponse {
        private Boolean success;
        private String status;
        private Integer testsPassed;
        private Integer totalTests;
        private String runtime;
        private String runtimeBeats;
        private String memory;
        private String memoryBeats;
        private String message;
    }
}

