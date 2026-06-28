package com.cairn.agents.impl;

import com.cairn.agents.AbstractDomainAgent;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/** WHY: Handles general conversational queries, greetings, and off-topic discussions. */
@Component
public class ConversationalAgent extends AbstractDomainAgent {
  public ConversationalAgent(VectorStore vectorStore) {
    super(vectorStore);
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
