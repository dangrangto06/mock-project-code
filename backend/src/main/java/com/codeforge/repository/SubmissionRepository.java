package com.codeforge.repository;

import com.codeforge.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByUserIdOrderBySubmittedAtDesc(Long userId);
    List<Submission> findByExerciseIdOrderBySubmittedAtDesc(Long exerciseId);
    List<Submission> findTop5ByUserIdOrderBySubmittedAtDesc(Long userId);
}

