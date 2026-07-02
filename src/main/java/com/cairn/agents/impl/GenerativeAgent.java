package com.cairn.agents.impl;

import com.cairn.agents.AbstractDomainAgent;
import com.cairn.agents.dto.AgentRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/**
 * WHY: The Generative Agent specializes in creative content generation — writing emails, code,
 * documentation, stories, and other forms of structured text output. It uses higher temperature
 * reasoning and emphasizes output quality and formatting.
 */
@Component
public class GenerativeAgent extends AbstractDomainAgent {
  public GenerativeAgent(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
    super(vectorStore, chatClientBuilder);
  }

  @Override
  public String getDomainName() {
    return "generative";
  }

  @Override
  protected String getSystemPrompt() {
    return """
        You are a creative writing and content generation assistant. You specialize in:
        1. Writing professional emails and messages
        2. Generating code snippets and documentation
        3. Creating marketing copy, blog posts, and articles
        4. Writing technical documentation and README files
        5. Crafting templates and boilerplate content

        RULES:
        - Always format output appropriately (use markdown, code blocks, etc.)
        - When writing code, specify the language and add comments
        - For emails, include Subject, To, and Body sections
        - Ask clarifying questions if the user's request is vague
        - Provide multiple options/variations when appropriate
        - Maintain the user's specified tone (formal, casual, technical, etc.)
        """;
  }

  @Override
  public String handle(AgentRequest request) {
    log.info("GenerativeAgent creating content for conversation {}", request.conversationId());

    String context = retrieveContext(request.messageContent());

    // Include history so the user can iterate on generated content
    StringBuilder historyContext = new StringBuilder();
    if (request.conversationHistory() != null && !request.conversationHistory().isEmpty()) {
      historyContext.append("\n\nPrevious drafts and feedback:\n");
      int start = Math.max(0, request.conversationHistory().size() - 4);
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
            s -> s.text(getSystemPrompt() + "\n\nReference material:\n" + context + historyContext))
        .user(request.messageContent())
        .call()
        .content();
  }
}
