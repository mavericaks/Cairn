package com.cairn.agents;

import com.cairn.agents.dto.AgentRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * WHY: Template Method design pattern. Provides the scaffolding for building standard LLM prompts
 * while allowing concrete subclasses to define their specific system instructions.
 */
public abstract class AbstractDomainAgent implements DomainAgent {

  protected final Logger log = LoggerFactory.getLogger(getClass());

  protected final VectorStore vectorStore;

  protected AbstractDomainAgent(VectorStore vectorStore) {
    this.vectorStore = vectorStore;
  }

  /** Retrieves the domain-specific system prompt that governs the agent's persona and rules. */
  protected abstract String getSystemPrompt();

  @Override
  public String handle(AgentRequest request) {
    log.info(
        "Agent '{}' handling request for conversation {}",
        getDomainName(),
        request.conversationId());

    // 1. RAG: Retrieve context from VectorStore
    String context = retrieveContext(request.messageContent());

    // 2. Build augmented prompt
    String augmentedPrompt =
        String.format(
            "System: %s\n\nContext:\n%s\n\nUser: %s",
            getSystemPrompt(), context, request.messageContent());

    // In a real implementation with Spring AI, we would call ChatClient here.
    // For now, we return a mock response containing the augmented prompt to prove RAG works.
    return String.format(
        "[%s Agent] RAG Augmented Prompt:\n%s", getDomainName().toUpperCase(), augmentedPrompt);
  }

  protected String retrieveContext(String query) {
    // WHY: We search for the top 3 most relevant document chunks based on cosine similarity.
    List<Document> documents =
        vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(3).build());

    if (documents.isEmpty()) {
      return "No relevant context found.";
    }

    return documents.stream().map(Document::getText).collect(Collectors.joining("\n---\n"));
  }
}
