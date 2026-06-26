package com.cairn.agents;

import com.cairn.agents.dto.AgentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WHY: Template Method design pattern. Provides the scaffolding for building standard LLM prompts
 * while allowing concrete subclasses to define their specific system instructions.
 */
public abstract class AbstractDomainAgent implements DomainAgent {

  protected final Logger log = LoggerFactory.getLogger(getClass());

  /** Retrieves the domain-specific system prompt that governs the agent's persona and rules. */
  protected abstract String getSystemPrompt();

  @Override
  public String handle(AgentRequest request) {
    log.info(
        "Agent '{}' handling request for conversation {}",
        getDomainName(),
        request.conversationId());

    // In a real implementation with Spring AI, we would:
    // 1. Build a prompt object combining getSystemPrompt(), conversationHistory, and messageContent
    // 2. Add any domain-specific tool calls (Epic 7)
    // 3. Call ChatClient
    // 4. Return the stream

    // For now, we return a mock response that proves the correct agent was invoked.
    return String.format(
        "[%s Agent] Received: %s", getDomainName().toUpperCase(), request.messageContent());
  }
}
