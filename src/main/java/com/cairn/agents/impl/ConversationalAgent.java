package com.cairn.agents.impl;

import com.cairn.agents.AbstractDomainAgent;
import com.cairn.agents.dto.AgentRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/**
 * WHY: The Conversational Agent handles general chat, small talk, and questions that don't fit
 * neatly into other domains. It prioritizes conversation history and multi-turn coherence over
 * document retrieval or tool use.
 */
@Component
public class ConversationalAgent extends AbstractDomainAgent {
  public ConversationalAgent(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
    super(vectorStore, chatClientBuilder);
  }

  @Override
  public String getDomainName() {
    return "conversational";
  }

  @Override
  protected String getSystemPrompt() {
    return """
        You are Cairn, a friendly and knowledgeable AI assistant. You handle general
        questions, explanations, creative tasks, and casual conversation.

        RULES:
        - Be concise but thorough
        - Use markdown formatting for better readability (headers, lists, code blocks)
        - If the user asks about something you can help with using tools or documents,
          suggest they rephrase their question (e.g., "Try asking me to 'calculate ...'
          or 'find information about ...'")
        - Remember context from the conversation history provided
        - Be honest when you don't know something
        """;
  }

  @Override
  public String handle(AgentRequest request) {
    log.info(
        "ConversationalAgent handling general request for conversation {}",
        request.conversationId());

    // WHY: Conversational agent emphasizes conversation history over RAG
    StringBuilder fullContext = new StringBuilder();

    // Include conversation history for continuity
    if (request.conversationHistory() != null && !request.conversationHistory().isEmpty()) {
      fullContext.append("Conversation so far:\n");
      request
          .conversationHistory()
          .forEach(
              msg ->
                  fullContext
                      .append(msg.getRole().name())
                      .append(": ")
                      .append(msg.getContent())
                      .append("\n"));
      fullContext.append("\n");
    }

    // Light RAG context (only if something relevant exists)
    String ragContext = retrieveContext(request.messageContent());
    if (!"No relevant context found.".equals(ragContext)) {
      fullContext.append("Relevant background knowledge:\n").append(ragContext);
    }

    return chatClient
        .prompt()
        .system(s -> s.text(getSystemPrompt() + "\n\n" + fullContext))
        .user(request.messageContent())
        .call()
        .content();
  }
}
