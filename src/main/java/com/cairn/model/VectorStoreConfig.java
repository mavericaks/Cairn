package com.cairn.model;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * WHY: Configures Spring AI's native VectorStore implementation. We inject our custom
 * LocalEmbeddingModelAdapter here so that all vector search/persistence uses our zero-cost CPU
 * embeddings.
 */
@Configuration
public class VectorStoreConfig {

  @Bean
  public VectorStore vectorStore(
      JdbcTemplate jdbcTemplate, LocalEmbeddingModelAdapter embeddingModel) {
    // WHY: We enable automatic schema generation. Since we are using
    // Flyway, it's generally best practice to manage schemas there,
    // but Spring AI provides a very specific schema for its vector_store table.
    // We'll let it manage its own table to ensure version compatibility with the framework.
    return PgVectorStore.builder(jdbcTemplate, embeddingModel).initializeSchema(true).build();
  }
}
