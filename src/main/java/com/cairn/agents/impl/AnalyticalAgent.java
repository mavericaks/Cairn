package com.cairn.agents.impl;

import com.cairn.agents.AbstractDomainAgent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/** WHY: Handles analytical and reasoning-heavy prompts. */
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
    return "You are an analytical assistant. Your task is to breakdown data, identify trends, and summarize numerical information.";
  }
}
