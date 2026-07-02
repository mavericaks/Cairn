package com.cairn.tools;

/**
 * WHY: Enum representing the lifecycle state of a tool execution. Enforces strict transitions for
 * Human-in-the-Loop approvals.
 */
public enum ToolStatus {
  PENDING_APPROVAL,
  APPROVED,
  EXECUTED,
  FAILED,
  REJECTED
}
