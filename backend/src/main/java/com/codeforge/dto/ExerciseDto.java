package com.codeforge.dto;

import lombok.*;

// import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseDto {
    private Long id;
    private String title;
    private String difficulty;
    private List<String> topics;
    private String acceptance;
    private String status; // 'solved', 'in-progress', 'unattempted'
    private Boolean saved;
    private Boolean isMine;
    private String author;
    private String updatedDate;
    private String description;
    private String constraints;
    private String starterCode;
    private List<TestCaseDto> testCases;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestCaseDto {
        private Long id;
        private String name;
        private String input;
        private String expected;
        private String yourOutput;
        private Boolean passed;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateExerciseRequest {
        private String title;
        private String statement;
        private String difficulty;
        private List<String> topics;
        private String language;
        private String timeLimit;
        private String memoryLimit;
    }
}

