package com.cairn.agents.impl;

import com.cairn.agents.AbstractDomainAgent;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/** WHY: Handles creative requests like generating code, writing essays, or brainstorming. */
@Component
public class GenerativeAgent extends AbstractDomainAgent {
  public GenerativeAgent(VectorStore vectorStore) {
    super(vectorStore);
  }

  @Override
  public String getDomainName() {
    return "generative";
  }

  @Override
  protected String getSystemPrompt() {
    return "You are a generative assistant. You excel at writing code, drafting documents, and creative brainstorming.";
  }
}
