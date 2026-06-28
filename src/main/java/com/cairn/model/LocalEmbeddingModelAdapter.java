package com.cairn.model;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.stereotype.Component;

/**
 * WHY: Adapter pattern to bridge our custom DJL-based local CPU embeddings into the standard Spring
 * AI ecosystem. By implementing EmbeddingModel, we can inject this directly into Spring AI's
 * VectorStore abstractions, getting zero-cost, local embeddings automatically.
 */
@Component
public class LocalEmbeddingModelAdapter implements EmbeddingModel {

  private final LocalEmbeddingService localEmbeddingService;

  public LocalEmbeddingModelAdapter(LocalEmbeddingService localEmbeddingService) {
    this.localEmbeddingService = localEmbeddingService;
  }

  @Override
  public float[] embed(Document document) {
    try {
      return localEmbeddingService.embed(document.getText());
    } catch (Exception e) {
      throw new RuntimeException("Failed to embed document via DJL adapter", e);
    }
  }

  @Override
  public EmbeddingResponse call(EmbeddingRequest request) {
    List<String> contents = request.getInstructions();
    try {
      List<Embedding> embeddingList = new java.util.ArrayList<>();
      for (int i = 0; i < contents.size(); i++) {
        float[] embedding = localEmbeddingService.embed(contents.get(i));
        embeddingList.add(new Embedding(embedding, i));
      }

      EmbeddingResponseMetadata metadata = new EmbeddingResponseMetadata();
      return new EmbeddingResponse(embeddingList, metadata);
    } catch (Exception e) {
      throw new RuntimeException("Failed to embed batch via DJL adapter", e);
    }
  }
}
