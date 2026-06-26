package com.cairn.agents.dto;

import com.cairn.agents.MessageRole;
import java.time.Instant;
import java.util.UUID;

/** WHY: DTO for Message (SDE Standard #1). Resolves routedDomainId to a name for the frontend. */
public record MessageDto(
    UUID id,
    UUID conversationId,
    MessageRole role,
    String content,
    String routedDomainName,
    Float routingScore,
    Integer tokenCount,
    Integer durationMs,
    Instant createdAt) {}
