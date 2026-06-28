package com.cairn.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the LocalEmbeddingService to ensure the DJL integration successfully loads the PyTorch
 * model and computes 384-dimensional vectors.
 *
 * <p>WHY: These tests verify: (1) correct dimensionality, (2) null/blank rejection, (3) thread
 * safety via concurrent embedding, (4) health reporting.
 */
class LocalEmbeddingServiceTest {

  private static final Logger log = LoggerFactory.getLogger(LocalEmbeddingServiceTest.class);
  private LocalEmbeddingService embeddingService;

  @BeforeEach
  void setUp() {
    // WHY: Build the properties and service manually for unit testing.
    // This avoids needing a full Spring context.
    log.info("Starting LocalEmbeddingService initialization for tests...");
    CairnEmbeddingProperties properties = new CairnEmbeddingProperties();
    embeddingService = new LocalEmbeddingService(properties);
    embeddingService.initModel();
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
    float[] embedding = embeddingService.embed(text);

    // Assert
    assertThat(embedding)
        .as("Embedding should not be null")
        .isNotNull()
        .as("all-MiniLM-L6-v2 should return exactly 384 dimensions")
        .hasSize(384);

    // WHY: Verify the model actually computed something — not all zeros.
    assertThat(embedding[0]).as("First dimension should be non-zero").isNotEqualTo(0.0f);
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

  @Test
  void shouldReportHealthyAfterSuccessfulInit() {
    // WHY: Consumers and health indicators rely on isHealthy() to check
    // if the service is usable before calling embed().
    assertThat(embeddingService.isHealthy())
        .as("Service should report healthy after successful model load")
        .isTrue();
  }

  @Test
  void shouldReturnCorrectDimensions() {
    assertThat(embeddingService.getDimensions())
        .as("Should return the configured dimension count")
        .isEqualTo(384);
  }

  @Test
  void shouldProduceDifferentEmbeddingsForDifferentTexts() {
    // WHY: Verifies the model is actually encoding semantic meaning,
    // not returning a constant vector.
    float[] embedding1 = embeddingService.embed("Hello, how are you?");
    float[] embedding2 = embeddingService.embed("Calculate the revenue for Q3.");

    assertThat(embedding1)
        .as("Different texts should produce different embeddings")
        .isNotEqualTo(embedding2);
  }

  @Test
  void shouldBeThreadSafeUnderConcurrentAccess() throws InterruptedException {
    // WHY: With virtual threads enabled, multiple requests will call embed()
    // simultaneously. This test proves our per-call Predictor strategy works.
    int threadCount = 5;
    String[] texts = {
      "Analyze the data",
      "Search for documents",
      "Hello there",
      "Execute this command",
      "Write a summary"
    };
    float[][] results = new float[threadCount][];
    Exception[] errors = new Exception[threadCount];

    Thread[] threads = new Thread[threadCount];
    for (int i = 0; i < threadCount; i++) {
      final int index = i;
      threads[i] =
          Thread.ofVirtual()
              .name("embed-test-" + i)
              .start(
                  () -> {
                    try {
                      results[index] = embeddingService.embed(texts[index]);
                    } catch (Exception e) {
                      errors[index] = e;
                    }
                  });
    }

    for (Thread thread : threads) {
      thread.join(30_000); // 30s timeout per thread
    }

    // Assert all threads completed without error
    for (int i = 0; i < threadCount; i++) {
      assertThat(errors[i]).as("Thread %d should not have thrown an exception", i).isNull();
      assertThat(results[i])
          .as("Thread %d should have produced a 384-dim embedding", i)
          .isNotNull()
          .hasSize(384);
    }

    // WHY: Verify different inputs produced different outputs (not cross-contaminated).
    assertThat(results[0])
        .as("Different inputs should not produce identical embeddings (cross-contamination check)")
        .isNotEqualTo(results[1]);
  }
}
