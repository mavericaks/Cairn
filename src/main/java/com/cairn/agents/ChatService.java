package com.cairn.agents;

import com.cairn.agents.dto.AgentRequest;
import com.cairn.agents.dto.ChatRequest;
import com.cairn.observability.Audited;
import com.cairn.routing.DomainContextCacheService;
import com.cairn.routing.DomainRouter;
import com.cairn.routing.RoutingResult;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * WHY: Business logic layer between the Controller (HTTP/SSE logic) and the Orchestrator (Agent
 * routing logic). Handles database persistence (Conversations/Messages) transactionally, and
 * integrates the Redis context cache for conversation memory.
 */
@Service
public class ChatService {

  private static final Logger log = LoggerFactory.getLogger(ChatService.class);

  private static final int CONTEXT_WINDOW_SIZE = 10;

  private final ConversationRepository conversationRepository;
  private final MessageRepository messageRepository;
  private final AgentOrchestrator orchestrator;
  private final DomainRouter domainRouter;
  private final DomainContextCacheService contextCache;

  public ChatService(
      ConversationRepository conversationRepository,
      MessageRepository messageRepository,
      AgentOrchestrator orchestrator,
      DomainRouter domainRouter,
      DomainContextCacheService contextCache) {
    this.conversationRepository = conversationRepository;
    this.messageRepository = messageRepository;
    this.orchestrator = orchestrator;
    this.domainRouter = domainRouter;
    this.contextCache = contextCache;
  }

  /**
   * Main entry point for processing a chat message. Transactional to ensure both user and assistant
   * messages are saved atomically.
   */
  @Transactional
  @Audited(action = "processChatMessage")
  public String processMessage(UUID userId, ChatRequest request) {

    Conversation conversation;
    if (request.conversationId() == null) {
      conversation = new Conversation(userId);
      conversation.setTitle(truncateForTitle(request.message()));
      conversationRepository.save(conversation);
    } else {
      conversation =
          conversationRepository
              .findById(request.conversationId())
              .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

      // Authorization: ensure the user owns this conversation
      if (!conversation.getUserId().equals(userId)) {
        throw new IllegalStateException("Not authorized to access this conversation");
      }
    }

    // 1. Save User Message
    Message userMessage = new Message(MessageRole.USER, request.message());
    conversation.addMessage(userMessage);

    // 2. Route the message to a domain
    RoutingResult route = domainRouter.route(request.message());
    userMessage.setRoutingScore((float) route.score());
    messageRepository.save(userMessage);

    log.info(
        "Routed to domain '{}' with score {} in {}ms",
        route.domainName(),
        String.format("%.4f", route.score()),
        route.latencyMs());

    // 3. Build context from Redis cache (hot) + DB history (cold)
    String cachedContext =
        contextCache.getContext(userId.toString(), route.domainName()).orElse("");

    List<Message> history = conversation.getMessages();
    if (history.size() > CONTEXT_WINDOW_SIZE) {
      history = history.subList(history.size() - CONTEXT_WINDOW_SIZE, history.size());
    }

    // 4. Build the agent request with full context
    AgentRequest agentRequest =
        new AgentRequest(userId, conversation.getId(), request.message(), route, history);

    // 5. Delegate to the agent orchestrator
    String assistantResponse = orchestrator.process(agentRequest);

    // 6. Save Assistant Message
    Message assistantMsg = new Message(MessageRole.ASSISTANT, assistantResponse);
    conversation.addMessage(assistantMsg);
    messageRepository.save(assistantMsg);

    // 7. Update Redis context cache with latest exchange
    String contextUpdate = "User: " + request.message() + "\nAssistant: " + assistantResponse;
    String updatedContext =
        cachedContext.isEmpty() ? contextUpdate : cachedContext + "\n---\n" + contextUpdate;

    // Trim context if it gets too long (keep last ~4000 chars)
    if (updatedContext.length() > 4000) {
      updatedContext = updatedContext.substring(updatedContext.length() - 4000);
    }
    contextCache.saveContext(userId.toString(), route.domainName(), updatedContext);

    return assistantResponse;
  }

  /** WHY: Generate a meaningful title from the first message instead of "New Conversation". */
  private String truncateForTitle(String message) {
    if (message == null || message.isBlank()) {
      return "New Conversation";
    }
    String cleaned = message.trim();
    if (cleaned.length() <= 60) {
      return cleaned;
    }
    return cleaned.substring(0, 57) + "...";
  }
}
