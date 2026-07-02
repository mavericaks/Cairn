package com.cairn.tools;

import java.util.UUID;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

/**
 * WHY: Configuration for tools that perform destructive or sensitive actions. These tools require
 * Human-in-the-Loop (HITL) approval before actual execution.
 */
@Configuration
public class UnsafeToolsConfig {

  private static final Logger log = LoggerFactory.getLogger(UnsafeToolsConfig.class);

  private final ToolExecutionService toolExecutionService;

  public UnsafeToolsConfig(ToolExecutionService toolExecutionService) {
    this.toolExecutionService = toolExecutionService;
  }

  public record SqlRequest(String query) {}

  public record SqlResponse(String result) {}

  /** WHY: A destructive tool that executes SQL. Must be gated by HITL. */
  @Bean
  @Description("Executes a SQL query on the database. Use this for administrative tasks.")
  public Function<SqlRequest, SqlResponse> executeSql() {
    return request -> {
      log.info("LLM requested executeSql: {}", request.query());

      // Delegate to the ToolExecutionService for HITL check
      // For simplicity, we assume messageId is passed somehow, but since Spring AI doesn't pass
      // context to the function easily, we use a placeholder or thread-local for now.
      // In a real implementation, we'd use a custom FunctionCallback or ThreadLocal to pass
      // conversation context.
      UUID fakeMessageId = UUID.randomUUID();

      ToolStatus status =
          toolExecutionService.requestExecution(
              fakeMessageId, "executeSql", request.toString(), true);

      if (status == ToolStatus.PENDING_APPROVAL) {
        return new SqlResponse(
            "Action paused. Tell the user you are awaiting admin approval for this SQL execution.");
      }

      // If we get here, it means it was somehow pre-approved or executed (out of band).
      // For now, this synchronous call always returns pending if it's the first time.
      return new SqlResponse("SQL Executed successfully (Mock).");
    };
  }
}
