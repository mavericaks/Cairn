package com.cairn.agents.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * WHY: DTO for incoming chat requests (SDE Standard #1). Validates that the message is not blank.
 */
public record ChatRequest(
    UUID conversationId, // Can be null for a new conversation
    @NotBlank(message = "Message content cannot be blank") String message) {}
