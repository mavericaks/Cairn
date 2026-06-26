package com.cairn.agents.dto;

import java.time.Instant;
import java.util.UUID;

/** WHY: DTO for Conversation (SDE Standard #1). Ensures JPA entities don't leak. */
public record ConversationDto(
    UUID id,
    UUID userId,
    String title,
    String lastDomainName,
    Instant createdAt,
    Instant updatedAt) {}
