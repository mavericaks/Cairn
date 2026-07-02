package com.cairn.analytics;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * WHY: Admin-only endpoint for querying the immutable audit log. Supports filtering by event type,
 * user, and time range. The audit_log table is append-only and serves as the system's compliance
 * and forensics layer.
 */
@RestController
@RequestMapping("/api/v1/admin/audit-log")
public class AuditLogController {

  private final JdbcTemplate jdbcTemplate;

  public AuditLogController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Returns paginated audit log entries with optional filters.
   *
   * @param eventType Optional filter by event type (CHAT_COMPLETED, TOOL_EXECUTED, etc.)
   * @param limit Max results (default 50, max 200)
   * @param offset Pagination offset (default 0)
   */
  @GetMapping
  public Map<String, Object> getAuditLog(
      @RequestParam(required = false) String eventType,
      @RequestParam(defaultValue = "50") int limit,
      @RequestParam(defaultValue = "0") int offset) {

    int safeLimit = Math.min(limit, 200);

    List<Map<String, Object>> entries;
    long totalCount;

    if (eventType != null && !eventType.isBlank()) {
      entries =
          jdbcTemplate.queryForList(
              """
              SELECT id, event_type, event_source, user_id, payload, created_at
              FROM audit_log
              WHERE event_type = ?
              ORDER BY created_at DESC
              LIMIT ? OFFSET ?
              """,
              eventType,
              safeLimit,
              offset);

      totalCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM audit_log WHERE event_type = ?", Long.class, eventType);
    } else {
      entries =
          jdbcTemplate.queryForList(
              """
              SELECT id, event_type, event_source, user_id, payload, created_at
              FROM audit_log
              ORDER BY created_at DESC
              LIMIT ? OFFSET ?
              """,
              safeLimit,
              offset);

      totalCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_log", Long.class);
    }

    return Map.of(
        "entries", entries,
        "totalCount", totalCount,
        "limit", safeLimit,
        "offset", offset,
        "hasMore", offset + safeLimit < totalCount);
  }
}
