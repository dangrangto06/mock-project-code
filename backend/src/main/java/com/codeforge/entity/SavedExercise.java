package com.codeforge.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA entity representing a bookmarked / saved problem for a user account.
 *
 * <p>Enforces a unique constraint across {@code (user_id, exercise_id)} to prevent
 * duplicate bookmarks.</p>
 */
@Entity
@Table(name = "saved_exercises", uniqueConstraints = {
        @UniqueConstraint(name = "uk_saved_exercise_user_exercise", columnNames = {"user_id", "exercise_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

