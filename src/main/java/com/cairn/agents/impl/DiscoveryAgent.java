package com.cairn.agents.impl;

import com.cairn.agents.AbstractDomainAgent;
import com.cairn.agents.dto.AgentRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/**
 * WHY: The Discovery Agent specializes in knowledge retrieval and document Q&A. It performs a
 * deeper RAG search (top 5 chunks instead of default 3) and explicitly presents source information
 * to the user. This agent demonstrates the RAG pipeline working end-to-end.
 */
@Component
public class DiscoveryAgent extends AbstractDomainAgent {
  public DiscoveryAgent(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
    super(vectorStore, chatClientBuilder);
  }

  @Override
  public String getDomainName() {
    return "discovery";
  }

  @Override
  protected String getSystemPrompt() {
    return """
        You are a knowledge discovery assistant. Your role is to help users find information
        from uploaded documents and the knowledge base.

        RULES:
        - Always ground your answers in the provided context
        - If the context doesn't contain relevant information, say so clearly
        - Cite which part of the context your answer comes from
        - If you're uncertain, state your confidence level
        - Suggest follow-up questions the user might want to ask
        - Format your responses with clear headings and bullet points for readability
        """;
  }

  @Override
  public String handle(AgentRequest request) {
    log.info(
        "DiscoveryAgent performing deep RAG search for conversation {}", request.conversationId());

    // WHY: Discovery agent searches deeper (top 5) and includes similarity scores
    List<Document> documents =
        vectorStore.similaritySearch(
            SearchRequest.builder().query(request.messageContent()).topK(5).build());

    String ragContext;
    if (documents.isEmpty()) {
      ragContext =
          "No documents found in the knowledge base matching this query. "
              + "The user may need to upload relevant documents first.";
    } else {
      String chunks =
          documents.stream()
              .map(
                  doc -> {
                    String source = doc.getMetadata().getOrDefault("source", "unknown").toString();
                    return "--- Source: " + source + " ---\n" + doc.getText();
                  })
              .collect(Collectors.joining("\n\n"));
      ragContext = "Found " + documents.size() + " relevant document chunks:\n\n" + chunks;
    }

    final String finalRagContext = ragContext;

    // Include conversation history for multi-turn document Q&A
    StringBuilder historyContext = new StringBuilder();
    if (request.conversationHistory() != null && !request.conversationHistory().isEmpty()) {
      historyContext.append("\n\nPrevious conversation context:\n");
      int start = Math.max(0, request.conversationHistory().size() - 6);
      request
          .conversationHistory()
          .subList(start, request.conversationHistory().size())
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
                        + "\n\nKnowledge Base Results:\n"
                        + finalRagContext
                        + historyContext))
        .user(request.messageContent())
        .call()
        .content();
  }
}
