package com.cairn.agents.impl;

import com.cairn.agents.AbstractDomainAgent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/**
 * WHY: Handles requests that require executing actions (e.g., sending an email, kicking off a
 * build).
 */
@Component
public class ExecutionAgent extends AbstractDomainAgent {
  public ExecutionAgent(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
    super(vectorStore, chatClientBuilder);
  }

  @Override
  public String getDomainName() {
    return "execution";
  }

  @Override
  protected String getSystemPrompt() {
    return "You are an execution assistant. You help the user perform actions by using the tools available to you.";
  }

  @Override
  protected String[] getTools() {
    return new String[] {"calculateMath", "getCurrentTime", "executeSql"};
  }
}
