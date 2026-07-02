package com.cairn.analytics;

import java.time.Instant;
import java.util.UUID;

/**
 * WHY: Published to Kafka when any tool is invoked by the LLM. Tracks tool usage patterns, approval
 * rates, and execution performance.
 */
public record ToolExecutedEvent(
    String toolName,
    UUID userId,
    String status,
    long durationMs,
    boolean requiresApproval,
    Instant timestamp) {

  public static ToolExecutedEvent of(
      String toolName, UUID userId, String status, long durationMs, boolean requiresApproval) {
    return new ToolExecutedEvent(
        toolName, userId, status, durationMs, requiresApproval, Instant.now());
  }
}
