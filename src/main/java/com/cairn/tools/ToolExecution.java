package com.cairn.tools;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * WHY: Entity representing a tool execution request. Tracks both safe (immediately executed) and
 * unsafe (pending approval) tool executions for auditability.
 */
@Entity
@Table(name = "tool_executions")
public class ToolExecution {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "message_id", nullable = false)
  private UUID messageId;

  @Column(name = "tool_name", nullable = false, length = 100)
  private String toolName;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "input_params", columnDefinition = "jsonb")
  private String inputParams;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "output", columnDefinition = "jsonb")
  private String output;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ToolStatus status;

  @Column(name = "requires_approval", nullable = false)
  private boolean requiresApproval;

  @Column(name = "approved_by")
  private UUID approvedBy;

  @CreationTimestamp
  @Column(name = "requested_at", nullable = false, updatable = false)
  private ZonedDateTime requestedAt;

  @Column(name = "resolved_at")
  private ZonedDateTime resolvedAt;

  @Column(name = "executed_at")
  private ZonedDateTime executedAt;

  @Column(name = "duration_ms")
  private Integer durationMs;

  public ToolExecution() {}

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getMessageId() {
    return messageId;
  }

  public void setMessageId(UUID messageId) {
    this.messageId = messageId;
  }

  public String getToolName() {
    return toolName;
  }

  public void setToolName(String toolName) {
    this.toolName = toolName;
  }

  public String getInputParams() {
    return inputParams;
  }

  public void setInputParams(String inputParams) {
    this.inputParams = inputParams;
  }

  public String getOutput() {
    return output;
  }

  public void setOutput(String output) {
    this.output = output;
  }

  public ToolStatus getStatus() {
    return status;
  }

  public void setStatus(ToolStatus status) {
    this.status = status;
  }

  public boolean isRequiresApproval() {
    return requiresApproval;
  }

  public void setRequiresApproval(boolean requiresApproval) {
    this.requiresApproval = requiresApproval;
  }

  public UUID getApprovedBy() {
    return approvedBy;
  }

  public void setApprovedBy(UUID approvedBy) {
    this.approvedBy = approvedBy;
  }

  public ZonedDateTime getRequestedAt() {
    return requestedAt;
  }

  public void setRequestedAt(ZonedDateTime requestedAt) {
    this.requestedAt = requestedAt;
  }

  public ZonedDateTime getResolvedAt() {
    return resolvedAt;
  }

  public void setResolvedAt(ZonedDateTime resolvedAt) {
    this.resolvedAt = resolvedAt;
  }

  public ZonedDateTime getExecutedAt() {
    return executedAt;
  }

  public void setExecutedAt(ZonedDateTime executedAt) {
    this.executedAt = executedAt;
  }

  public Integer getDurationMs() {
    return durationMs;
  }

  public void setDurationMs(Integer durationMs) {
    this.durationMs = durationMs;
  }
}
