package com.cairn.tools;

import java.util.UUID;
import org.springframework.modulith.events.Externalized;

/**
 * WHY: Event published when a tool execution is requested. If requiresApproval is true, the tool is
 * paused and this event triggers the HITL queue.
 */
@Externalized("cairn.tool.approvals")
public record ToolExecutionRequestedEvent(
    UUID executionId,
    UUID messageId,
    String toolName,
    String inputParams,
    boolean requiresApproval) {}
