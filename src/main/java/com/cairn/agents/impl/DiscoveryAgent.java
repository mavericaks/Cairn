package com.cairn.agents.impl;

import com.cairn.agents.AbstractDomainAgent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

/** WHY: Handles queries requiring documentation search, semantic discovery, and RAG retrieval. */
@Component
public class DiscoveryAgent extends AbstractDomainAgent {

  private final ChatModel chatModel;

  public DiscoveryAgent(
      VectorStore vectorStore, ChatClient.Builder chatClientBuilder, ChatModel chatModel) {
    super(vectorStore, chatClientBuilder);
    this.chatModel = chatModel;
  }

  @Override
  protected String retrieveContext(String query) {
    log.info("Generating hypothetical document for query: '{}'", query);
    // WHY: HyDE (Hypothetical Document Embeddings). Instead of embedding the user's short query,
    // we ask the LLM to hallucinate a perfect answer, and we embed THAT answer.
    // The hallucination will semantically match the real document in the vector store much better
    // than the short query would, drastically improving retrieval accuracy.
    String hypotheticalAnswer =
        chatClient
            .prompt()
            .user(
                "Please write a short, hypothetical passage that directly answers the following query. Write it as if it were an excerpt from official documentation. Query: "
                    + query)
            .call()
            .content();

    log.debug("Generated HyDE answer: {}", hypotheticalAnswer);

    // Search using the hypothetical answer
    return super.retrieveContext(hypotheticalAnswer);
  }

  @Override
  public String getDomainName() {
    return "discovery";
  }

  @Override
  protected String getSystemPrompt() {
    return "You are a discovery assistant. Your goal is to find accurate information and present it clearly to the user.";
  }
}
