package com.cairn.analytics;

import java.time.Instant;
import java.util.UUID;

/**
 * WHY: Structured event published to Kafka after every chat completion. Contains all the metadata
 * needed for routing analytics, usage tracking, and audit logging.
 */
public record ChatCompletedEvent(
    UUID userId,
    UUID conversationId,
    String domainName,
    double routingScore,
    long routingLatencyMs,
    int responseTokenCount,
    Instant timestamp) {

  public static ChatCompletedEvent of(
      UUID userId,
      UUID conversationId,
      String domainName,
      double routingScore,
      long routingLatencyMs,
      String response) {
    return new ChatCompletedEvent(
        userId,
        conversationId,
        domainName,
        routingScore,
        routingLatencyMs,
        response != null ? response.split("\\s+").length : 0,
        Instant.now());
  }
}
