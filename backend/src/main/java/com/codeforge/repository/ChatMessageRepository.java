package com.codeforge.repository;

import com.codeforge.entity.ChatMessage;
import com.codeforge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Spring Data JPA repository for {@link ChatMessage} entity persistence.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    List<ChatMessage> findByUserOrderByCreatedAtAsc(User user);

    @Transactional
    @Modifying
    @Query("DELETE FROM ChatMessage c WHERE c.conversationId = :conversationId")
    void deleteByConversationId(String conversationId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ChatMessage c WHERE c.user = :user")
    void deleteByUser(User user);
}
