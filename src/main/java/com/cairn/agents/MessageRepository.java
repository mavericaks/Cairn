package com.cairn.agents;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** WHY: Spring Data JPA repository for Messages. */
public interface MessageRepository extends JpaRepository<Message, UUID> {

  /**
   * Retrieves messages for a conversation, ordered chronologically (oldest first for LLM context).
   *
   * @param conversationId The conversation's UUID
   * @param pageable Pagination parameters
   * @return A page of messages
   */
  Page<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId, Pageable pageable);

  /**
   * Retrieves messages for a conversation, ordered reverse-chronologically (newest first for UI).
   *
   * @param conversationId The conversation's UUID
   * @param pageable Pagination parameters
   * @return A page of messages
   */
  Page<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

  /**
   * Complex native query fulfilling SDE Standard #10. Finds all messages across all conversations
   * for a user where the message routed to a specific domain. Useful for analytics or "show me all
   * my SQL queries".
   */
  @Query(
      value =
          "SELECT m.* FROM messages m "
              + "JOIN conversations c ON m.conversation_id = c.id "
              + "WHERE c.user_id = :userId AND m.routed_domain_id = :domainId",
      countQuery =
          "SELECT count(*) FROM messages m "
              + "JOIN conversations c ON m.conversation_id = c.id "
              + "WHERE c.user_id = :userId AND m.routed_domain_id = :domainId",
      nativeQuery = true)
  Page<Message> findUserMessagesByDomain(
      @Param("userId") UUID userId, @Param("domainId") UUID domainId, Pageable pageable);
}
