package com.codeforge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing user-submitted feedback, feature requests, and bug reports.
 *
 * <p>Persisted in the {@code feedbacks} table so administrators can review user impressions,
 * evaluate suggested enhancements, and address reported issues.</p>
 */
@Entity
@Table(name = "feedbacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Optional authenticated user reference if the feedback was submitted by a registered user.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Submitter display name.
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Submitter contact email (auto-filled with the authenticated user's email).
     */
    @Column(nullable = false, length = 150)
    private String email;

    /**
     * Brief subject or category classification (e.g. General, Bug Report, Feature Request).
     */
    @Column(length = 200)
    private String subject;

    /**
     * Detailed feedback message content.
     */
    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    /**
     * Operational status: "NEW", "REVIEWED", "RESOLVED".
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "NEW";

    /**
     * Timestamp when the feedback was dispatched.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "NEW";
        }
    }
}
