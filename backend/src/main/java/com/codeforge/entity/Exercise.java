package com.codeforge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing a programming problem or challenge in the CodeForge catalog.
 *
 * <p>Maps to the {@code exercises} table. Contains the problem statement, algorithm constraints,
 * categorization topics, difficulty tier, multi-language starter templates, execution limits,
 * and associated test cases.</p>
 *
 * <p><strong>Relationships:</strong></p>
 * <ul>
 *   <li>{@link #author}: Many-to-One lazy relationship with {@link User} (creator of custom problems).</li>
 *   <li>{@link #testCases}: One-to-Many cascade relationship with {@link TestCase}.</li>
 * </ul>
 */
@Entity
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {

    /**
     * Unique identifier for the challenge (e.g., 104, 105, 106).
     * Assigned explicitly to align with standardized problem numbering.
     */
    @Id
    private Long id;

    /**
     * Human-readable problem title (e.g., "Pair Sum").
     */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * Difficulty tier rating: {@code "Easy"}, {@code "Medium"}, or {@code "Hard"}.
     */
    @Column(nullable = false, length = 50)
    private String difficulty;

    /**
     * Comma-separated list of algorithmic topics and data structures (e.g., "Arrays, Hash map").
     */
    @Column(nullable = false, length = 255)
    private String topics;

    /**
     * Platform-wide submission acceptance percentage for this specific problem.
     */
    @Builder.Default
    private BigDecimal acceptanceRate = new BigDecimal("78.40");

    /**
     * Comprehensive problem statement formatted in Markdown.
     */
    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    /**
     * Algorithmic constraints specifying input ranges and limits in Markdown.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String constraints;

    /**
     * Default starter code template for Python 3.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String starterCodePython;

    /**
     * Default starter code template for JavaScript (Node.js).
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String starterCodeJavascript;

    /**
     * Default starter code template for C++ (GCC).
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String starterCodeCpp;

    /**
     * Default starter code template for Java (OpenJDK).
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String starterCodeJava;

    /**
     * Maximum execution time limit in milliseconds before terminating with Time Limit Exceeded (TLE).
     */
    @Builder.Default
    private Integer timeLimitMs = 1000;

    /**
     * Maximum allowable memory allocation in megabytes before Memory Limit Exceeded (MLE).
     */
    @Builder.Default
    private Integer memoryLimitMb = 256;

    /**
     * Flag indicating whether this challenge was created by a user (true) or is system-provided (false).
     */
    @Builder.Default
    private Boolean isCustom = false;

    /**
     * The author/contributor who created this challenge, if user-created.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    /**
     * Associated test cases used to evaluate user submissions.
     */
    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TestCase> testCases = new ArrayList<>();

    /**
     * Timestamp marking when the exercise was published.
     */
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
