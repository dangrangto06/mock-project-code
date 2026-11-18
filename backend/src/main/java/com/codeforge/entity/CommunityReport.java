package com.codeforge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a user-submitted flag/report on a community discussion thread
 * or a comment/reply.
 */
@Entity
@Table(name = "community_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type of content reported: "DISCUSSION" or "REPLY".
     */
    @Column(name = "report_type", nullable = false, length = 20)
    private String reportType;

    /**
     * ID of the reported discussion or discussion reply.
     */
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    /**
     * User who filed this report.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    /**
     * Category of violation (e.g. "OFF_TOPIC", "SPAM", "HARASSMENT", "INAPPROPRIATE", "OTHER").
     */
    @Column(nullable = false, length = 100)
    private String reason;

    /**
     * Optional additional details provided by the reporter.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String details;

    /**
     * Workflow status: "PENDING", "DISMISSED", "RESOLVED".
     */
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
    }
}
