package com.codeforge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Column(nullable = false, length = 50)
    private String language; // 'python', 'javascript', 'cpp', 'java'

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String sourceCode;

    @Column(nullable = false, length = 50)
    private String status; // 'Accepted', 'Wrong Answer', 'Time Limit Exceeded'

    private Integer runtimeMs;

    private BigDecimal memoryMb;

    private BigDecimal runtimeBeatsPct;

    private BigDecimal memoryBeatsPct;

    @Builder.Default
    private Integer testsPassed = 0;

    @Builder.Default
    private Integer totalTests = 0;

    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();
}

