package com.cairn.agents.dto;

/**
 * WHY: Defines the structure of SSE events sent to the frontend. Types help the frontend parse
 * routing metadata separately from the actual token stream.
 */
public record ChatStreamEvent(EventType type, String content, String domain) {

  public enum EventType {
    ROUTING,
    TOKEN,
    DONE,
    ERROR
  }
}
