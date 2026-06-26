package com.cairn.agents;

import com.cairn.agents.dto.AgentRequest;
import com.cairn.agents.dto.ChatRequest;
import com.cairn.observability.Audited;
import com.cairn.routing.DomainRouter;
import com.cairn.routing.RoutingResult;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * WHY: Business logic layer between the Controller (HTTP/SSE logic) and the Orchestrator (Agent
 * routing logic). Handles database persistence (Conversations/Messages) transactionally.
 */
@Service
public class ChatService {

  private final ConversationRepository conversationRepository;
  private final MessageRepository messageRepository;
  private final AgentOrchestrator orchestrator;
  private final DomainRouter domainRouter;

  public ChatService(
      ConversationRepository conversationRepository,
      MessageRepository messageRepository,
      AgentOrchestrator orchestrator,
      DomainRouter domainRouter) {
    this.conversationRepository = conversationRepository;
    this.messageRepository = messageRepository;
    this.orchestrator = orchestrator;
    this.domainRouter = domainRouter;
  }

  /**
   * Main entry point for processing a chat message. Transactional to ensure both user and assistant
   * messages are saved securely.
   */
  @Transactional
  @Audited(action = "processChatMessage")
  public String processMessage(UUID userId, ChatRequest request) {

    Conversation conversation;
    if (request.conversationId() == null) {
      conversation = new Conversation(userId);
      conversation.setTitle("New Conversation"); // A real app would generate this with LLM
      conversationRepository.save(conversation);
    } else {
      conversation =
          conversationRepository
              .findById(request.conversationId())
              .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));

      // Basic authorization check (Epic 5 will use real security)
      if (!conversation.getUserId().equals(userId)) {
        throw new IllegalStateException("Not authorized to access this conversation");
      }
    }

    // 1. Save User Message
    Message userMessage = new Message(MessageRole.USER, request.message());
    conversation.addMessage(userMessage);

    // 2. We do a manual routing call here just to record it on the message.
    // In a full async streaming implementation, this would be more integrated.
    RoutingResult route = domainRouter.route(request.message());
    userMessage.setRoutingScore((float) route.score());
    // userMessage.setRoutedDomain(...) would require a Domain entity lookup, skipping for brevity
    // in this stub

    messageRepository.save(userMessage);

    // 3. Delegate to orchestrator
    // We only pass the last 5 messages for context window management
    List<Message> history = conversation.getMessages();
    if (history.size() > 5) {
      history = history.subList(history.size() - 5, history.size());
    }

    AgentRequest agentRequest =
        new AgentRequest(userId, conversation.getId(), request.message(), route, history);

    String assistantResponse = orchestrator.process(agentRequest);

    // 4. Save Assistant Message
    Message assistantMsg = new Message(MessageRole.ASSISTANT, assistantResponse);
    conversation.addMessage(assistantMsg);
    messageRepository.save(assistantMsg);

    return assistantResponse;
  }
}
