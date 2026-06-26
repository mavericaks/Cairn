package com.cairn.agents;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** WHY: Spring Data JPA repository for Conversations. Exposes standard CRUD and pagination. */
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

  /**
   * Retrieves all conversations for a specific user with pagination.
   *
   * @param userId The user's UUID
   * @param pageable Pagination parameters
   * @return A page of conversations
   */
  Page<Conversation> findByUserId(UUID userId, Pageable pageable);
}
