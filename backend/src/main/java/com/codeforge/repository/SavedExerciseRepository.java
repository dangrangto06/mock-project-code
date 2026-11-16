package com.codeforge.repository;

import com.codeforge.entity.SavedExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link SavedExercise} entity persistence.
 */
@Repository
public interface SavedExerciseRepository extends JpaRepository<SavedExercise, Long> {

    /**
     * Checks if a user has bookmarked a specific exercise.
     */
    boolean existsByUserIdAndExerciseId(Long userId, Long exerciseId);

    /**
     * Finds a saved exercise entry for a given user and exercise pair.
     */
    Optional<SavedExercise> findByUserIdAndExerciseId(Long userId, Long exerciseId);

    /**
     * Deletes a saved exercise entry by user ID and exercise ID.
     */
    void deleteByUserIdAndExerciseId(Long userId, Long exerciseId);

    /**
     * Retrieves all saved exercise entries bookmarked by a user.
     */
    List<SavedExercise> findByUserId(Long userId);

    /**
     * Counts the total number of exercises bookmarked by a user.
     */
    long countByUserId(Long userId);
}

