package com.codeforge.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity representing a verification test case for an algorithmic exercise.
 *
 * <p>Maps to the {@code test_cases} table. Each test case supplies standardized input data
 * and the expected deterministic output used to validate submitted source code.</p>
 *
 * <p><strong>Sample vs Hidden Evaluation Tests:</strong></p>
 * <ul>
 *   <li><b>Sample Tests ({@code isSample = true}):</b> Publicly visible in the IDE test console
 *       for rapid local iteration and debugging.</li>
 *   <li><b>Hidden Tests ({@code isSample = false}):</b> Evaluated only upon final submission
 *       to guard against hardcoded solution exploits and edge case oversights.</li>
 * </ul>
 */
@Entity
@Table(name = "test_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCase {

    /**
     * Primary key identifier for the test case (auto-incremented identity).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The parent exercise to which this test case belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    /**
     * Descriptive label for the test case (e.g., "Case 1", "Edge Case - Negative Numbers").
     */
    @Column(nullable = false, length = 100)
    private String caseName;

    /**
     * Standard input data fed into the solution script or executable via stdin.
     */
    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String inputData;

    /**
     * Expected standard output against which solution stdout is compared.
     */
    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String expectedOutput;

    /**
     * Distinguishes public sample cases visible in the IDE from private judging test cases.
     */
    @Builder.Default
    private Boolean isSample = true;
}
