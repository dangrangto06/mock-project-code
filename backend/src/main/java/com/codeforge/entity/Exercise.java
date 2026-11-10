package com.codeforge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exercise {

    @Id
    private Long id; // e.g. 104, 105, 106

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 50)
    private String difficulty; // 'Easy', 'Medium', 'Hard'

    @Column(nullable = false, length = 255)
    private String topics; // e.g. "Arrays, Hash map"

    @Builder.Default
    private BigDecimal acceptanceRate = new BigDecimal("78.40");

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String constraints;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String starterCodePython;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String starterCodeJavascript;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String starterCodeCpp;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String starterCodeJava;

    @Builder.Default
    private Integer timeLimitMs = 1000;

    @Builder.Default
    private Integer memoryLimitMb = 256;

    @Builder.Default
    private Boolean isCustom = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TestCase> testCases = new ArrayList<>();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

