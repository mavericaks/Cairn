package com.cairn.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * WHY: Consumes chat and tool events from Kafka and aggregates them into analytics tables. Uses
 * UPSERT (INSERT ... ON CONFLICT UPDATE) to maintain running aggregates without requiring
 * pre-existing rows.
 *
 * <p>This consumer demonstrates a real-world Kafka pattern: decoupled analytics that don't slow
 * down the main request path. The chat flow publishes events asynchronously, and this consumer
 * processes them at its own pace.
 */
@Component
public class AnalyticsConsumer {

  private static final Logger log = LoggerFactory.getLogger(AnalyticsConsumer.class);

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public AnalyticsConsumer(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  /**
   * WHY: Consumes ChatCompletedEvents and updates two analytics tables: 1. analytics_routing —
   * domain hit counts, avg score, avg latency (hourly buckets) 2. analytics_usage — per-user
   * per-day message and token counts
   */
  @KafkaListener(topics = "cairn.events.chat", groupId = "cairn-analytics-group")
  public void onChatCompleted(String message) {
    try {
      ChatCompletedEvent event = objectMapper.readValue(message, ChatCompletedEvent.class);

      // 1. Upsert routing analytics (hourly bucket)
      jdbcTemplate.update(
          """
          INSERT INTO analytics_routing (domain_name, hour_bucket, hit_count, avg_score, avg_latency_ms)
          VALUES (?, date_trunc('hour', NOW()), 1, ?, ?)
          ON CONFLICT (domain_name, hour_bucket)
          DO UPDATE SET
            hit_count = analytics_routing.hit_count + 1,
            avg_score = (analytics_routing.avg_score * analytics_routing.hit_count + EXCLUDED.avg_score)
                        / (analytics_routing.hit_count + 1),
            avg_latency_ms = (analytics_routing.avg_latency_ms * analytics_routing.hit_count + EXCLUDED.avg_latency_ms)
                             / (analytics_routing.hit_count + 1)
          """,
          event.domainName(),
          event.routingScore(),
          (double) event.routingLatencyMs());

      // 2. Upsert usage analytics (daily bucket)
      jdbcTemplate.update(
          """
          INSERT INTO analytics_usage (user_id, day_bucket, message_count, token_count)
          VALUES (?::uuid, CURRENT_DATE, 1, ?)
          ON CONFLICT (user_id, day_bucket)
          DO UPDATE SET
            message_count = analytics_usage.message_count + 1,
            token_count = analytics_usage.token_count + EXCLUDED.token_count
          """,
          event.userId().toString(),
          (long) event.responseTokenCount());

      log.debug(
          "Analytics updated: domain={}, user={}, tokens={}",
          event.domainName(),
          event.userId(),
          event.responseTokenCount());

    } catch (Exception e) {
      log.error("Failed to process chat analytics event: {}", e.getMessage(), e);
    }
  }

  /**
   * WHY: Consumes ToolExecutedEvents and updates tool execution statistics. Tracks call counts,
   * approval/rejection rates, and average execution duration.
   */
  @KafkaListener(topics = "cairn.events.tools", groupId = "cairn-analytics-group")
  public void onToolExecuted(String message) {
    try {
      ToolExecutedEvent event = objectMapper.readValue(message, ToolExecutedEvent.class);

      int approved = "APPROVED".equals(event.status()) ? 1 : 0;
      int rejected = "REJECTED".equals(event.status()) ? 1 : 0;

      jdbcTemplate.update(
          """
          INSERT INTO analytics_tools (tool_name, day_bucket, call_count, approval_count, rejection_count, avg_duration_ms)
          VALUES (?, CURRENT_DATE, 1, ?, ?, ?)
          ON CONFLICT (tool_name, day_bucket)
          DO UPDATE SET
            call_count = analytics_tools.call_count + 1,
            approval_count = analytics_tools.approval_count + EXCLUDED.approval_count,
            rejection_count = analytics_tools.rejection_count + EXCLUDED.rejection_count,
            avg_duration_ms = (analytics_tools.avg_duration_ms * analytics_tools.call_count + EXCLUDED.avg_duration_ms)
                              / (analytics_tools.call_count + 1)
          """,
          event.toolName(),
          approved,
          rejected,
          (double) event.durationMs());

      log.debug("Tool analytics updated: tool={}, status={}", event.toolName(), event.status());

    } catch (Exception e) {
      log.error("Failed to process tool analytics event: {}", e.getMessage(), e);
    }
  }
}
