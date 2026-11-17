package com.codeforge.repository;

import com.codeforge.entity.Discussion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link Discussion} entities.
 */
@Repository
public interface DiscussionRepository extends JpaRepository<Discussion, Long> {

    /**
     * Finds all discussions ordered by creation date descending.
     */
    @Query("SELECT d FROM Discussion d LEFT JOIN FETCH d.author ORDER BY d.createdAt DESC")
    List<Discussion> findAllByOrderByCreatedAtDesc();

    /**
     * Finds all discussions within a category ordered by creation date descending.
     */
    @Query("SELECT d FROM Discussion d LEFT JOIN FETCH d.author WHERE d.category = :category ORDER BY d.createdAt DESC")
    List<Discussion> findByCategoryOrderByCreatedAtDesc(String category);

    long countByModerationStatus(String moderationStatus);

    @Query("SELECT d FROM Discussion d LEFT JOIN FETCH d.author WHERE d.moderationStatus = :moderationStatus ORDER BY d.createdAt DESC")
    List<Discussion> findByModerationStatusOrderByCreatedAtDesc(String moderationStatus);
}

