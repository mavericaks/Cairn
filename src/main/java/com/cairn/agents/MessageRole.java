package com.cairn.agents;

/**
 * Maps to the Postgres 'message_role' ENUM.
 *
 * <p>WHY: Strongly typed roles correspond to the LLM's understanding of who sent what.
 */
public enum MessageRole {
  USER,
  ASSISTANT,
  SYSTEM,
  TOOL
}
