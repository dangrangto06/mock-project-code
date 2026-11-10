package com.codeforge.repository;

import com.codeforge.entity.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
    
    List<Exercise> findByDifficultyIgnoreCase(String difficulty);

    @Query("SELECT e FROM Exercise e WHERE " +
           "(:query IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) OR STR(e.id) LIKE CONCAT('%', :query, '%')) AND " +
           "(:difficulty IS NULL OR LOWER(e.difficulty) = LOWER(:difficulty)) AND " +
           "(:topic IS NULL OR LOWER(e.topics) LIKE LOWER(CONCAT('%', :topic, '%')))")
    List<Exercise> searchExercises(@Param("query") String query,
                                  @Param("difficulty") String difficulty,
                                  @Param("topic") String topic);
}

