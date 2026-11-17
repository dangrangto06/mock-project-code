package com.codeforge.repository;

import com.codeforge.entity.DiscussionUpvote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link DiscussionUpvote} entities.
 */
@Repository
public interface DiscussionUpvoteRepository extends JpaRepository<DiscussionUpvote, Long> {

    boolean existsByDiscussionIdAndUserId(Long discussionId, Long userId);

    Optional<DiscussionUpvote> findByDiscussionIdAndUserId(Long discussionId, Long userId);

    void deleteByDiscussionIdAndUserId(Long discussionId, Long userId);

    long countByDiscussionId(Long discussionId);
}

