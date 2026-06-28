package com.cairn.agents.impl;

import com.cairn.agents.AbstractDomainAgent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/** WHY: Handles internal system queries, routing logic debugging, and observability questions. */
@Component
public class SystemAgent extends AbstractDomainAgent {
  public SystemAgent(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
    super(vectorStore, chatClientBuilder);
  }

  @Override
  public String getDomainName() {
    return "system";
  }

  @Override
  protected String getSystemPrompt() {
    return "You are a system assistant. You provide information about the application's configuration, health, and user settings.";
  }
}
