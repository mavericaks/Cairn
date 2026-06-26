package com.cairn.agents;

import com.cairn.agents.dto.ConversationDto;
import com.cairn.agents.dto.MessageDto;
import com.cairn.model.dto.PageResponse;
import com.cairn.observability.Audited;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * WHY: Service layer for managing conversation history. Enforces ownership and handles DTO mapping.
 */
@Service
public class ConversationService {

  private final ConversationRepository conversationRepository;
  private final MessageRepository messageRepository;
  private final ConversationMapper mapper;

  public ConversationService(
      ConversationRepository conversationRepository,
      MessageRepository messageRepository,
      ConversationMapper mapper) {
    this.conversationRepository = conversationRepository;
    this.messageRepository = messageRepository;
    this.mapper = mapper;
  }

  @Audited
  @Transactional(readOnly = true)
  public PageResponse<ConversationDto> getUserConversations(UUID userId, Pageable pageable) {
    Page<Conversation> page = conversationRepository.findByUserId(userId, pageable);
    return PageResponse.of(page.map(mapper::toDto));
  }

  @Audited
  @Transactional(readOnly = true)
  public PageResponse<MessageDto> getConversationMessages(
      UUID userId, UUID conversationId, Pageable pageable) {

    // Verify ownership
    Conversation conversation =
        conversationRepository
            .findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

    if (!conversation.getUserId().equals(userId)) {
      throw new IllegalStateException("Not authorized to access this conversation");
    }

    Page<Message> page =
        messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable);
    return PageResponse.of(page.map(mapper::toDto));
  }

  @Audited
  @Transactional
  public void deleteConversation(UUID userId, UUID conversationId) {
    Conversation conversation =
        conversationRepository
            .findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

    if (!conversation.getUserId().equals(userId)) {
      throw new IllegalStateException("Not authorized to delete this conversation");
    }

    // CascadeType.ALL handles deleting the messages
    conversationRepository.delete(conversation);
  }
}
