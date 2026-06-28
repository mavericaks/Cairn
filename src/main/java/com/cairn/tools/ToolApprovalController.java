package com.cairn.tools;

import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** WHY: REST API for administrators to view and manage the HITL tool approval queue. */
@RestController
@RequestMapping("/api/v1/tools/approvals")
public class ToolApprovalController {

  private final ToolExecutionRepository repository;
  private final ApplicationEventPublisher eventPublisher;

  public ToolApprovalController(
      ToolExecutionRepository repository, ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
  }

  /** WHY: Retrieves a paginated list of pending tool executions. */
  @GetMapping
  public ResponseEntity<Page<ToolExecution>> getPendingApprovals(Pageable pageable) {
    return ResponseEntity.ok(repository.findByStatus(ToolStatus.PENDING_APPROVAL, pageable));
  }

  /**
   * WHY: Approves a pending tool execution. Changes status to APPROVED and publishes an event to
   * Kafka so the consumer can execute it asynchronously.
   */
  @PostMapping("/{id}/approve")
  @Transactional
  public ResponseEntity<Void> approveExecution(@PathVariable UUID id) {
    ToolExecution execution =
        repository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Execution not found"));

    if (execution.getStatus() != ToolStatus.PENDING_APPROVAL) {
      throw new IllegalStateException("Execution is not pending approval");
    }

    execution.setStatus(ToolStatus.APPROVED);
    execution.setResolvedAt(java.time.ZonedDateTime.now());

    // Mocking the admin user ID since we don't have security context yet
    UUID adminId = UUID.randomUUID();
    execution.setApprovedBy(adminId);

    repository.save(execution);

    // Publish event (auto-externalized to Kafka)
    eventPublisher.publishEvent(new ToolExecutionApprovedEvent(id, adminId));

    return ResponseEntity.ok().build();
  }

  /** WHY: Rejects a pending tool execution. No Kafka event needed since it's just rejected. */
  @PostMapping("/{id}/reject")
  @Transactional
  public ResponseEntity<Void> rejectExecution(@PathVariable UUID id) {
    ToolExecution execution =
        repository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Execution not found"));

    if (execution.getStatus() != ToolStatus.PENDING_APPROVAL) {
      throw new IllegalStateException("Execution is not pending approval");
    }

    execution.setStatus(ToolStatus.REJECTED);
    execution.setResolvedAt(java.time.ZonedDateTime.now());

    // Mocking the admin user ID
    execution.setApprovedBy(UUID.randomUUID());

    repository.save(execution);

    return ResponseEntity.ok().build();
  }
}
