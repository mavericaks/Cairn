package com.cairn.analytics;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * WHY: Admin-only REST endpoints that expose the analytics data aggregated by the Kafka consumers.
 * These power the admin dashboard with real-time routing accuracy, usage patterns, and tool
 * execution metrics.
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

  private final JdbcTemplate jdbcTemplate;

  public AnalyticsController(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Returns routing analytics: domain hit distribution, average confidence scores, and average
   * routing latency over the last N hours (default 24).
   */
  @GetMapping("/routing")
  public List<Map<String, Object>> getRoutingAnalytics(
      @RequestParam(defaultValue = "24") int hours) {
    return jdbcTemplate.queryForList(
        """
        SELECT
          domain_name,
          SUM(hit_count) AS total_hits,
          ROUND(AVG(avg_score)::numeric, 4) AS avg_confidence,
          ROUND(AVG(avg_latency_ms)::numeric, 1) AS avg_latency_ms
        FROM analytics_routing
        WHERE hour_bucket >= NOW() - MAKE_INTERVAL(hours => ?)
        GROUP BY domain_name
        ORDER BY total_hits DESC
        """,
        hours);
  }

  /**
   * Returns per-user usage analytics: messages sent and tokens consumed per day over the last N
   * days (default 7).
   */
  @GetMapping("/usage")
  public List<Map<String, Object>> getUsageAnalytics(@RequestParam(defaultValue = "7") int days) {
    return jdbcTemplate.queryForList(
        """
        SELECT
          user_id,
          day_bucket,
          message_count,
          token_count
        FROM analytics_usage
        WHERE day_bucket >= CURRENT_DATE - MAKE_INTERVAL(days => ?)
        ORDER BY day_bucket DESC, message_count DESC
        """,
        days);
  }

  /**
   * Returns tool execution analytics: call frequency, approval/rejection rates, and average
   * execution duration over the last N days (default 7).
   */
  @GetMapping("/tools")
  public List<Map<String, Object>> getToolAnalytics(@RequestParam(defaultValue = "7") int days) {
    return jdbcTemplate.queryForList(
        """
        SELECT
          tool_name,
          SUM(call_count) AS total_calls,
          SUM(approval_count) AS total_approvals,
          SUM(rejection_count) AS total_rejections,
          ROUND(AVG(avg_duration_ms)::numeric, 1) AS avg_duration_ms,
          CASE
            WHEN SUM(call_count) > 0
            THEN ROUND(SUM(approval_count)::numeric / SUM(call_count) * 100, 1)
            ELSE 0
          END AS approval_rate_pct
        FROM analytics_tools
        WHERE day_bucket >= CURRENT_DATE - MAKE_INTERVAL(days => ?)
        GROUP BY tool_name
        ORDER BY total_calls DESC
        """,
        days);
  }

  /** Returns a summary dashboard with key platform metrics. */
  @GetMapping("/dashboard")
  public Map<String, Object> getDashboard() {
    Map<String, Object> totalMessages =
        jdbcTemplate.queryForMap(
            "SELECT COALESCE(SUM(message_count), 0) AS total FROM analytics_usage");
    Map<String, Object> totalTokens =
        jdbcTemplate.queryForMap(
            "SELECT COALESCE(SUM(token_count), 0) AS total FROM analytics_usage");
    Map<String, Object> totalToolCalls =
        jdbcTemplate.queryForMap(
            "SELECT COALESCE(SUM(call_count), 0) AS total FROM analytics_tools");
    Map<String, Object> avgLatency =
        jdbcTemplate.queryForMap(
            "SELECT COALESCE(ROUND(AVG(avg_latency_ms)::numeric, 1), 0) AS avg FROM analytics_routing");

    return Map.of(
        "totalMessages", totalMessages.get("total"),
        "totalTokens", totalTokens.get("total"),
        "totalToolCalls", totalToolCalls.get("total"),
        "avgRoutingLatencyMs", avgLatency.get("avg"));
  }
}
