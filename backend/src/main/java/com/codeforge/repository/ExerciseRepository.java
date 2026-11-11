package com.codeforge.repository;

import com.codeforge.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Exercise} entity management.
 *
 * <p>Enables full lifecycle persistence and multi-criteria dynamic filtering
 * across algorithm challenges by difficulty tier, category tags, and search keywords.</p>
 */
@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    
    /**
     * Finds all exercises matching a given difficulty tier, ignoring case sensitivity.
     *
     * @param difficulty The difficulty level (e.g. "Easy", "Medium", "Hard").
     * @return List of matching {@link Exercise} entities.
     */
    List<Exercise> findByDifficultyIgnoreCase(String difficulty);

    /**
     * Executes a dynamic multi-predicate search across the problem catalog.
     *
     * <p>Allows combining title substring matches, problem ID string matching, difficulty filter,
     * and topic keywords in a single JPQL query with null-safe parameter evaluations.</p>
     *
     * @param query      Keyword query matching against exercise title or numerical ID.
     * @param difficulty Difficulty filter ("Easy", "Medium", "Hard") or null for all.
     * @param topic      Algorithmic topic tag (e.g. "Arrays", "Hash map") or null for all.
     * @return Filtered list of {@link Exercise} entities.
     */
    @Query("SELECT e FROM Exercise e WHERE " +
           "(:query IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) OR STR(e.id) LIKE CONCAT('%', :query, '%')) AND " +
           "(:difficulty IS NULL OR LOWER(e.difficulty) = LOWER(:difficulty)) AND " +
           "(:topic IS NULL OR LOWER(e.topics) LIKE LOWER(CONCAT('%', :topic, '%')))")
    List<Exercise> searchExercises(@Param("query") String query,
                                  @Param("difficulty") String difficulty,
                                  @Param("topic") String topic);
}
