package com.cairn.tools;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** WHY: Repository for ToolExecution entities. */
public interface ToolExecutionRepository extends JpaRepository<ToolExecution, UUID> {

  Page<ToolExecution> findByStatus(ToolStatus status, Pageable pageable);
}
