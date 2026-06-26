package com.cairn.agents.dto;

import com.cairn.agents.Message;
import com.cairn.routing.RoutingResult;
import java.util.List;
import java.util.UUID;

/** WHY: SDE Standard #1. Encapsulates all data required for an agent to process a request. */
public record AgentRequest(
    UUID userId,
    UUID conversationId,
    String messageContent,
    RoutingResult routingResult,
    List<Message> conversationHistory) {}
