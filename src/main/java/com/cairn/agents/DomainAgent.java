package com.cairn.agents;

import com.cairn.agents.dto.AgentRequest;

/**
 * WHY: Defines the contract for all domain-specific AI agents. By using an interface, the
 * AgentOrchestrator can easily inject a list of all implementations and select the right one
 * dynamically without hardcoded switch statements.
 */
public interface DomainAgent {

  /** The name of the domain this agent is responsible for (e.g., "execution", "analytical"). */
  String getDomainName();

  /**
   * Processes the user's request using the agent's specific domain logic. Note: In a fully
   * implemented system, this would return a reactive Flux for streaming. For this stub, it returns
   * a static string. E3-T7 will handle the SSE streaming.
   *
   * @param request The complete context of the request
   * @return The agent's response string
   */
  String handle(AgentRequest request);
}
