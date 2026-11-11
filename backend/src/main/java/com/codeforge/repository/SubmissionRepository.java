package com.codeforge.repository;

import com.codeforge.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Submission} entity persistence.
 *
 * <p>Provides specialized queries to audit user submission histories, retrieve submissions
 * per challenge, and extract recent activity logs for user dashboard telemetry.</p>
 */
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    /**
     * Retrieves all submissions authored by a specific user, ordered chronologically descending.
     *
     * @param userId The ID of the submitting user.
     * @return Ordered list of {@link Submission} entities.
     */
    List<Submission> findByUserIdOrderBySubmittedAtDesc(Long userId);

    /**
     * Retrieves all submissions associated with an exercise, ordered chronologically descending.
     *
     * @param exerciseId The ID of the exercise.
     * @return Ordered list of {@link Submission} entities.
     */
    List<Submission> findByExerciseIdOrderBySubmittedAtDesc(Long exerciseId);

    /**
     * Retrieves the top 5 most recent submissions for a user to display in the dashboard activity widget.
     *
     * @param userId The ID of the user.
     * @return Up to 5 most recent {@link Submission} entities.
     */
    List<Submission> findTop5ByUserIdOrderBySubmittedAtDesc(Long userId);
}
