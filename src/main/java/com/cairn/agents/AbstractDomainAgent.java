package com.cairn.agents;

import com.cairn.agents.dto.AgentRequest;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
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
  protected final ChatClient chatClient;

  protected AbstractDomainAgent(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
    this.vectorStore = vectorStore;
    this.chatClient = chatClientBuilder.build();
  }

  /** Retrieves the domain-specific system prompt that governs the agent's persona and rules. */
  protected abstract String getSystemPrompt();

  /**
   * Returns a list of tool (function) names this agent is allowed to use. Override in specific
   * agents.
   */
  protected String[] getTools() {
    return new String[0];
  }

  @Override
  public String handle(AgentRequest request) {
    log.info(
        "Agent '{}' handling request for conversation {}",
        getDomainName(),
        request.conversationId());

    // 1. RAG: Retrieve context from VectorStore
    String context = retrieveContext(request.messageContent());

    // 2. Call LLM with Context and Tools
    String[] tools = getTools();

    return chatClient
        .prompt()
        .system(s -> s.text(getSystemPrompt() + "\n\nContext:\n" + context))
        .user(request.messageContent())
        .toolNames(tools) // Enable specific tools for this agent
        .call()
        .content();
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
