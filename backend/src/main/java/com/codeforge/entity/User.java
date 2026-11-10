package com.codeforge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(length = 50)
    @Builder.Default
    private String role = "Learner"; // 'Guest', 'Learner', 'Admin'

    @Builder.Default
    private Integer streakDays = 7;

    @Builder.Default
    private Integer solvedCount = 48;

    @Builder.Default
    private BigDecimal acceptanceRate = new BigDecimal("76.20");

    @Builder.Default
    private BigDecimal practiceTimeHours = new BigDecimal("12.50");

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

