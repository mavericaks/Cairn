package com.cairn.agents.impl;

import com.cairn.agents.AbstractDomainAgent;
import com.cairn.agents.dto.AgentRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/**
 * WHY: The Execution Agent handles requests that require performing real-world actions via tools.
 * This agent is the only one that has tool-calling capabilities enabled (calculateMath,
 * getCurrentTime, searchWeb, executeSql). It demonstrates the agentic tool-use pattern where the
 * LLM autonomously decides when to invoke functions.
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
    return """
        You are an execution assistant with access to real tools. You can:
        1. Calculate math expressions (use the calculateMath tool)
        2. Get the current time in any timezone (use the getCurrentTime tool)
        3. Search the web for information (use the searchWeb tool)
        4. Execute SQL queries on a database (use the executeSql tool — requires admin approval)

        RULES:
        - When the user asks you to calculate something, ALWAYS use the calculateMath tool
        - When the user asks about time, ALWAYS use the getCurrentTime tool
        - When asked to run SQL, use the executeSql tool (it will be queued for admin approval)
        - After receiving a tool result, present it clearly to the user
        - If a tool returns an error, explain what went wrong
        - You can chain multiple tool calls if needed
        """;
  }

  @Override
  protected String[] getTools() {
    return new String[] {"calculateMath", "getCurrentTime", "searchWeb", "executeSql"};
  }

  @Override
  public String handle(AgentRequest request) {
    log.info(
        "ExecutionAgent processing request with tools for conversation {}",
        request.conversationId());

    String context = retrieveContext(request.messageContent());

    return chatClient
        .prompt()
        .system(s -> s.text(getSystemPrompt() + "\n\nContext:\n" + context))
        .user(request.messageContent())
        .toolNames(getTools())
        .call()
        .content();
  }
}
