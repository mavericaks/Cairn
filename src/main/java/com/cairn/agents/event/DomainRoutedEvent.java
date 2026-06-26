package com.cairn.agents.event;

import java.util.UUID;

/**
 * WHY: Spring Application Event to notify other modules (like Observability) that a routing
 * decision was made and an agent handled the request. Decouples modules.
 */
public record DomainRoutedEvent(
    UUID userId, UUID conversationId, String domainName, float routingScore) {}
