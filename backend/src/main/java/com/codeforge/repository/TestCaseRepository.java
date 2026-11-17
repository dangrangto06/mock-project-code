package com.codeforge.repository;

import com.codeforge.entity.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link TestCase} entity operations.
 *
 * <p>Supports querying evaluation test cases by associated exercise identifier,
 * distinguishing between public sample test suites and full judging suites.</p>
 */
@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    /**
     * Retrieves all test cases associated with a specific exercise identifier.
     *
     * @param exerciseId The unique identifier of the target exercise.
     * @return List of all {@link TestCase} records for this exercise.
     */
    List<TestCase> findByExerciseId(Long exerciseId);

    /**
     * Retrieves only sample test cases (where {@code isSample = true}) for an exercise.
     *
     * <p>Used to populate the IDE test case runner tabs on the frontend.</p>
     *
     * @param exerciseId The unique identifier of the target exercise.
     * @return List of sample {@link TestCase} records for this exercise.
     */
    List<TestCase> findByExerciseIdAndIsSampleTrue(Long exerciseId);
}
