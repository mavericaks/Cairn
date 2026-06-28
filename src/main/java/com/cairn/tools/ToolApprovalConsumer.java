package com.cairn.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * WHY: Consumes HITL approval events from Kafka and executes the actual destructive logic after it
 * has been approved by an admin.
 */
@Component
public class ToolApprovalConsumer {

  private static final Logger log = LoggerFactory.getLogger(ToolApprovalConsumer.class);

  private final ToolExecutionRepository repository;
  private final ObjectMapper objectMapper;

  public ToolApprovalConsumer(ToolExecutionRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  /**
   * WHY: Listens for events on 'cairn.tool.approvals'. When an admin approves an execution, this
   * consumer picks it up, performs the action (e.g., executing SQL), and updates the DB.
   */
  @KafkaListener(topics = "cairn.tool.approvals", groupId = "cairn-tools-group")
  @Transactional
  public void consumeApproval(String eventJson) {
    try {
      log.info("Received Kafka event on cairn.tool.approvals: {}", eventJson);

      // The event might be ToolExecutionRequestedEvent or ToolExecutionApprovedEvent.
      // In a real system, we might use headers to differentiate, but we can just check if it's an
      // approval.
      if (!eventJson.contains("approvedBy")) {
        log.debug("Event is not an approval event, ignoring.");
        return;
      }

      ToolExecutionApprovedEvent event =
          objectMapper.readValue(eventJson, ToolExecutionApprovedEvent.class);

      ToolExecution execution =
          repository
              .findById(event.executionId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException("Execution not found: " + event.executionId()));

      if (execution.getStatus() != ToolStatus.APPROVED) {
        log.warn(
            "Execution {} is not in APPROVED state. Current state: {}",
            execution.getId(),
            execution.getStatus());
        return;
      }

      log.info(
          "EXECUTING TOOL '{}' OUT-OF-BAND (ID: {})", execution.getToolName(), execution.getId());

      // MOCK EXECUTION OF THE TOOL
      execution.setStatus(ToolStatus.EXECUTED);
      execution.setExecutedAt(java.time.ZonedDateTime.now());
      execution.setOutput("{\"result\": \"Successfully executed out-of-band via Kafka\"}");

      repository.save(execution);
      log.info("Tool execution {} marked as EXECUTED.", execution.getId());

    } catch (Exception e) {
      log.error("Failed to process tool approval event", e);
    }
  }
}
