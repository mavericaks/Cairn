package com.cairn.agents.impl;

import com.cairn.agents.AbstractDomainAgent;
import com.cairn.agents.dto.AgentRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/**
 * WHY: The System Agent handles meta-queries about the Cairn platform itself — questions about
 * capabilities, available domains, how to use features, and platform health. It serves as the "help
 * desk" agent.
 */
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
    return """
        You are the Cairn platform system agent. You help users understand and navigate
        the Cairn AI orchestration platform.

        PLATFORM CAPABILITIES:
        - **Analytical Agent**: SQL generation, data analysis, trend identification
        - **Execution Agent**: Tool calling (math, time, web search, SQL execution)
        - **Discovery Agent**: Document search, knowledge base Q&A (RAG)
        - **Generative Agent**: Content creation (emails, code, documentation)
        - **Conversational Agent**: General chat and questions
        - **System Agent**: Platform help and guidance (you!)

        FEATURES:
        - Semantic routing: Messages are automatically routed to the best agent
        - Document upload: Users can upload PDFs/TXT for RAG-grounded Q&A
        - Tool approval: Destructive tools require admin approval (HITL)
        - Conversation memory: Context is maintained across messages

        RULES:
        - Help users discover what the platform can do
        - Suggest which agent would handle their request best
        - Explain platform features when asked
        - If the user seems lost, provide example prompts they could try
        """;
  }

  @Override
  public String handle(AgentRequest request) {
    log.info("SystemAgent handling platform query for conversation {}", request.conversationId());

    return chatClient
        .prompt()
        .system(s -> s.text(getSystemPrompt()))
        .user(request.messageContent())
        .call()
        .content();
  }
}
