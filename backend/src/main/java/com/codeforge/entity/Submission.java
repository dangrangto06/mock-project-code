package com.codeforge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity recording a user code submission for an exercise.
 *
 * <p>Maps to the {@code submissions} table. Tracks the lifecycle, execution metrics,
 * and outcome of code submitted to the CodeForge execution engine.</p>
 *
 * <p><strong>Key Metrics Tracked:</strong></p>
 * <ul>
 *   <li><b>Outcome Status:</b> e.g. {@code "Accepted"}, {@code "Wrong Answer"}, {@code "Time Limit Exceeded"}, {@code "Runtime Error"}.</li>
 *   <li><b>Performance:</b> Execution runtime in milliseconds and memory consumption in megabytes.</li>
 *   <li><b>Efficiency Percentiles:</b> Runtime and memory beats percentiles comparing against other global submissions.</li>
 *   <li><b>Test Coverage:</b> Count of passed test cases vs total test cases evaluated.</li>
 * </ul>
 */
@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    /**
     * Primary key identifier for the submission record (auto-incremented identity).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who authored and submitted the solution code.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The target algorithmic exercise being solved.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    /**
     * Programming language utilized (e.g. {@code "python"}, {@code "javascript"}, {@code "cpp"}, {@code "java"}).
     */
    @Column(nullable = false, length = 50)
    private String language;

    /**
     * Complete source code submitted by the user.
     */
    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String sourceCode;

    /**
     * Final verdict of the judging pipeline (e.g. "Accepted", "Wrong Answer").
     */
    @Column(nullable = false, length = 50)
    private String status;

    /**
     * Average execution time consumed across all evaluated test cases in milliseconds.
     */
    private Integer runtimeMs;

    /**
     * Peak memory consumed during program execution in megabytes.
     */
    private BigDecimal memoryMb;

    /**
     * Percentile of submissions beaten in terms of runtime speed (0.00% to 100.00%).
     */
    private BigDecimal runtimeBeatsPct;

    /**
     * Percentile of submissions beaten in terms of memory efficiency (0.00% to 100.00%).
     */
    private BigDecimal memoryBeatsPct;

    /**
     * Number of test cases that matched expected output exactly.
     */
    @Builder.Default
    private Integer testsPassed = 0;

    /**
     * Total number of test cases evaluated during judging.
     */
    @Builder.Default
    private Integer totalTests = 0;

    /**
     * Timestamp marking when the submission was dispatched and recorded.
     */
    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();
}
