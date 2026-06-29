package com.cairn.agents.impl;

import com.cairn.agents.AbstractDomainAgent;
import com.cairn.agents.dto.AgentRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/**
 * WHY: The Analytical Agent specializes in data analysis, SQL generation, and numerical reasoning.
 * Unlike the generic conversational agent, this agent has a structured output format and domain
 * specific instructions for producing actionable analytical results.
 */
@Component
public class AnalyticalAgent extends AbstractDomainAgent {
  public AnalyticalAgent(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
    super(vectorStore, chatClientBuilder);
  }

  @Override
  public String getDomainName() {
    return "analytical";
  }

  @Override
  protected String getSystemPrompt() {
    return """
        You are an expert data analyst AI assistant. Your specialties:
        1. Writing SQL queries for PostgreSQL databases
        2. Analyzing data trends and patterns
        3. Performing statistical calculations
        4. Creating data summaries and reports

        RULES:
        - When asked to write SQL, ALWAYS wrap it in a ```sql code block
        - Explain your reasoning step-by-step before showing the query
        - If the user's request is ambiguous, ask clarifying questions about table structure
        - Always mention potential performance considerations for large datasets
        - Use CTEs (WITH clauses) for complex queries to improve readability
        """;
  }

  @Override
  public String handle(AgentRequest request) {
    log.info("AnalyticalAgent processing request for conversation {}", request.conversationId());

    // WHY: The analytical agent enriches the context with any relevant document chunks
    // (e.g., database schemas uploaded via RAG) before calling the LLM.
    String ragContext = retrieveContext(request.messageContent());

    // Build conversation history as context
    StringBuilder historyContext = new StringBuilder();
    if (request.conversationHistory() != null && !request.conversationHistory().isEmpty()) {
      historyContext.append("\n\nRecent conversation:\n");
      request
          .conversationHistory()
          .forEach(
              msg ->
                  historyContext
                      .append(msg.getRole().name())
                      .append(": ")
                      .append(msg.getContent())
                      .append("\n"));
    }

    return chatClient
        .prompt()
        .system(
            s ->
                s.text(
                    getSystemPrompt()
                        + "\n\nRelevant context from knowledge base:\n"
                        + ragContext
                        + historyContext))
        .user(request.messageContent())
        .call()
        .content();
  }
}
