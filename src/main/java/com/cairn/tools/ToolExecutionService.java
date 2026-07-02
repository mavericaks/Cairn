package com.cairn.tools;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * WHY: Manages the lifecycle of tool executions, enforcing HITL (Human-in-the-Loop) approvals for
 * destructive actions.
 */
@Service
public class ToolExecutionService {

  private static final Logger log = LoggerFactory.getLogger(ToolExecutionService.class);

  private final ToolExecutionRepository repository;
  private final ApplicationEventPublisher eventPublisher;

  public ToolExecutionService(
      ToolExecutionRepository repository, ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
  }

  /**
   * WHY: Records a tool execution request. If it requires approval, publishes an event to the Kafka
   * topic and leaves it in PENDING_APPROVAL state.
   */
  @Transactional
  public ToolStatus requestExecution(
      UUID messageId, String toolName, String inputParams, boolean requiresApproval) {
    log.info(
        "Tool execution requested: toolName={}, requiresApproval={}", toolName, requiresApproval);

    ToolExecution execution = new ToolExecution();
    execution.setMessageId(messageId);
    execution.setToolName(toolName);
    execution.setInputParams(inputParams);
    execution.setRequiresApproval(requiresApproval);

    if (requiresApproval) {
      execution.setStatus(ToolStatus.PENDING_APPROVAL);
      execution = repository.save(execution);

      // Publish event to Modulith (which auto-publishes to Kafka via @Externalized)
      ToolExecutionRequestedEvent event =
          new ToolExecutionRequestedEvent(
              execution.getId(), messageId, toolName, inputParams, requiresApproval);
      eventPublisher.publishEvent(event);
      log.info("Published ToolExecutionRequestedEvent for executionId={}", execution.getId());

      return ToolStatus.PENDING_APPROVAL;
    } else {
      // For safe tools, we just record it and return EXECUTED (for now).
      // In a real flow, the tool executes and THEN we record success/failure.
      // But we intercept the intent here.
      execution.setStatus(ToolStatus.EXECUTED);
      execution.setExecutedAt(java.time.ZonedDateTime.now());
      repository.save(execution);
      return ToolStatus.EXECUTED;
    }
  }
}
