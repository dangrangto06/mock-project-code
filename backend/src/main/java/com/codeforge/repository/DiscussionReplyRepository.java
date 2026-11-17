package com.codeforge.repository;

import com.codeforge.entity.DiscussionReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link DiscussionReply} entities.
 */
@Repository
public interface DiscussionReplyRepository extends JpaRepository<DiscussionReply, Long> {

    /**
     * Finds all replies for a given discussion ordered by creation date ascending.
     */
    @Query("SELECT r FROM DiscussionReply r LEFT JOIN FETCH r.author WHERE r.discussion.id = :discussionId ORDER BY r.createdAt ASC")
    List<DiscussionReply> findByDiscussionIdOrderByCreatedAtAsc(Long discussionId);
}

