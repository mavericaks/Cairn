package com.cairn.agents;

import com.cairn.agents.dto.ConversationDto;
import com.cairn.agents.dto.MessageDto;
import org.springframework.stereotype.Component;

/**
 * WHY: Manual mapper to enforce SDE Standard #1 (DTO Layer). Avoids MapStruct overhead for simple
 * transformations and safely resolves lazy-loaded associations (like Domain.getName()).
 */
@Component
public class ConversationMapper {

  public ConversationDto toDto(Conversation entity) {
    if (entity == null) {
      return null;
    }
    String lastDomainName =
        entity.getLastDomain() != null ? entity.getLastDomain().getName() : null;

    return new ConversationDto(
        entity.getId(),
        entity.getUserId(),
        entity.getTitle(),
        lastDomainName,
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public MessageDto toDto(Message entity) {
    if (entity == null) {
      return null;
    }
    String routedDomainName =
        entity.getRoutedDomain() != null ? entity.getRoutedDomain().getName() : null;

    return new MessageDto(
        entity.getId(),
        entity.getConversation().getId(),
        entity.getRole(),
        entity.getContent(),
        routedDomainName,
        entity.getRoutingScore(),
        entity.getTokenCount(),
        entity.getDurationMs(),
        entity.getCreatedAt());
  }
}
