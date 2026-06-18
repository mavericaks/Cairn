package com.cairn.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the LocalEmbeddingService to ensure the DJL integration successfully loads the PyTorch
 * model and computes 384-dimensional vectors.
 */
class LocalEmbeddingServiceTest {

  private static final Logger log = LoggerFactory.getLogger(LocalEmbeddingServiceTest.class);
  private LocalEmbeddingService embeddingService;

  @BeforeEach
  void setUp() {
    // WHY: Initialize the service manually for unit testing.
    // This will download the model to ~/.djl.ai on the first run.
    log.info("Starting LocalEmbeddingService initialization for tests...");
    embeddingService = new LocalEmbeddingService();
  }

  @AfterEach
  void tearDown() {
    if (embeddingService != null) {
      embeddingService.close();
    }
  }

  @Test
  void shouldReturn384DimensionalVectorForValidText() {
    // Arrange
    String text = "I need to analyze the quarterly financial report.";

    // Act
    List<Double> embedding = embeddingService.embed(text);

    // Assert
    assertThat(embedding)
        .as("Embedding list should not be null")
        .isNotNull()
        .as("all-MiniLM-L6-v2 should return exactly 384 dimensions")
        .hasSize(384);

    // Just verify it actually computed numbers
    assertThat(embedding.get(0)).as("First dimension should not be null").isNotNull();
  }

  @Test
  void shouldThrowExceptionForNullInput() {
    assertThatThrownBy(() -> embeddingService.embed(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot embed null or blank text");
  }

  @Test
  void shouldThrowExceptionForBlankInput() {
    assertThatThrownBy(() -> embeddingService.embed("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Cannot embed null or blank text");
  }
}
