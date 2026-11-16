package com.codeforge.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) representing an exercise submission record.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionDto {

    private Long id;
    private Long exerciseId;
    private String exerciseTitle;
    private String status;
    private String language;
    private String runtime;
    private String memory;
    private Integer runtimeMs;
    private BigDecimal memoryMb;
    private BigDecimal runtimeBeatsPct;
    private BigDecimal memoryBeatsPct;
    private Integer testsPassed;
    private Integer totalTests;
    private String sourceCode;
    private String date;
    private LocalDateTime submittedAt;
}

