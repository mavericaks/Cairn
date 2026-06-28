package com.cairn.agents;

import com.cairn.agents.dto.ConversationDto;
import com.cairn.agents.dto.MessageDto;
import com.cairn.model.dto.PageResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * WHY: REST API for the frontend to render the chat history sidebar and load messages for a given
 * conversation. Implements SDE Standard #2 (Pagination via PageResponse).
 */
@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

  private final ConversationService conversationService;

  public ConversationController(ConversationService conversationService) {
    this.conversationService = conversationService;
  }

  @GetMapping
  public PageResponse<ConversationDto> getConversations(
      @PageableDefault(
              size = 20,
              sort = "updatedAt",
              direction = org.springframework.data.domain.Sort.Direction.DESC)
          Pageable pageable) {
    String principal = SecurityContextHolder.getContext().getAuthentication().getName();
    UUID userId = UUID.fromString(principal);
    return conversationService.getUserConversations(userId, pageable);
  }

  @GetMapping("/{id}/messages")
  public PageResponse<MessageDto> getMessages(
      @PathVariable UUID id,
      @PageableDefault(
              size = 50,
              sort = "createdAt",
              direction = org.springframework.data.domain.Sort.Direction.ASC)
          Pageable pageable) {
    String principal = SecurityContextHolder.getContext().getAuthentication().getName();
    UUID userId = UUID.fromString(principal);
    return conversationService.getConversationMessages(userId, id, pageable);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteConversation(@PathVariable UUID id) {
    String principal = SecurityContextHolder.getContext().getAuthentication().getName();
    UUID userId = UUID.fromString(principal);
    conversationService.deleteConversation(userId, id);
    return ResponseEntity.noContent().build();
  }
}
