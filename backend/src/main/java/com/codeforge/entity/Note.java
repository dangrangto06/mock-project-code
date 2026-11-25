package com.codeforge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing a learner's personal note or algorithm pattern memo.
 *
 * <p>Persists study notes, code templates, takeaways, and tags associated with
 * specific algorithmic exercises or general coding techniques.</p>
 */
@Entity
@Table(name = "notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note {

    /**
     * Unique surrogate primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The owning learner account.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Subject or headline of the note.
     */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * Optional linked problem ID.
     */
    @Column(name = "problem_id")
    private Long problemId;

    /**
     * Optional linked problem title cache.
     */
    @Column(name = "problem_title", length = 255)
    private String problemTitle;

    /**
     * Comma-separated tag identifiers (e.g., "Two Pointers, Arrays, DP").
     */
    @Column(length = 255)
    private String tags;

    /**
     * Note body content containing markdown or code snippets.
     */
    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Timestamp of last note modification.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onSave() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
}

