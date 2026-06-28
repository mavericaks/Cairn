package com.cairn.agents.impl;

import com.cairn.agents.AbstractDomainAgent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/** WHY: Handles general conversational queries that don't fit into a specific domain. */
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
    return "You are a friendly conversational assistant. Keep your answers concise, helpful, and polite.";
  }
}
