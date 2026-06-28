package com.cairn.tools;

import java.util.UUID;
import org.springframework.modulith.events.Externalized;

/**
 * WHY: Event published when an admin manually approves a pending tool execution via the REST API.
 * Triggers the actual execution in the consumer.
 */
@Externalized("cairn.tool.approvals")
public record ToolExecutionApprovedEvent(UUID executionId, UUID approvedBy) {}
