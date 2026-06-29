package com.cairn.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * WHY: Persists every Kafka event as an immutable JSONB row in the audit_log table. This provides a
 * complete, tamper-evident record of all platform actions — critical for compliance, debugging, and
 * forensic analysis.
 *
 * <p>Key design: This consumer listens to ALL event topics. Every event is stored with its full
 * JSON payload, event type, and source context. The audit_log table is append-only — rows are never
 * updated or deleted.
 */
@Component
public class AuditLogConsumer {

  private static final Logger log = LoggerFactory.getLogger(AuditLogConsumer.class);

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public AuditLogConsumer(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  @KafkaListener(topics = "cairn.events.chat", groupId = "cairn-audit-group")
  public void auditChat(String message) {
    persistAuditEntry("CHAT_COMPLETED", "ChatService", message);
  }

  @KafkaListener(topics = "cairn.events.tools", groupId = "cairn-audit-group")
  public void auditTool(String message) {
    persistAuditEntry("TOOL_EXECUTED", "ToolExecutionService", message);
  }

  @KafkaListener(topics = "cairn.events.documents", groupId = "cairn-audit-group")
  public void auditDocument(String message) {
    persistAuditEntry("DOCUMENT_INGESTED", "DocumentIngestionService", message);
  }

  /**
   * WHY: Uses JSONB cast to store the event payload as a queryable JSON column. This allows admins
   * to filter audit logs using PostgreSQL JSON operators (e.g., payload->>'domainName').
   */
  private void persistAuditEntry(String eventType, String eventSource, String rawPayload) {
    try {
      // Extract userId from the payload if present
      String userId = null;
      try {
        var node = objectMapper.readTree(rawPayload);
        if (node.has("userId")) {
          userId = node.get("userId").asText();
        }
      } catch (Exception ignored) {
        // userId extraction is best-effort
      }

      jdbcTemplate.update(
          """
          INSERT INTO audit_log (event_type, event_source, user_id, payload)
          VALUES (?, ?, ?::uuid, ?::jsonb)
          """,
          eventType,
          eventSource,
          userId,
          rawPayload);

      log.debug("Audit entry persisted: type={}, source={}", eventType, eventSource);

    } catch (Exception e) {
      log.error("Failed to persist audit entry: type={}, error={}", eventType, e.getMessage(), e);
    }
  }
}
