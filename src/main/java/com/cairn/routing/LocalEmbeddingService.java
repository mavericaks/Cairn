package com.cairn.routing;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for generating local semantic embeddings using DJL and PyTorch.
 *
 * <p>WHY: By generating embeddings locally via CPU, we completely bypass network latency and API
 * costs. This makes intent classification (Semantic Routing) effectively instant (~20ms) and
 * entirely offline.
 *
 * <p><b>Thread Safety:</b> {@link ZooModel} is thread-safe (read-only model weights). {@link
 * Predictor} is NOT thread-safe (holds mutable inference buffers). Therefore, we share one ZooModel
 * and create a new Predictor per call via try-with-resources, which gives each thread its own
 * workspace with zero contention.
 */
@Service
public class LocalEmbeddingService {
  private static final Logger log = LoggerFactory.getLogger(LocalEmbeddingService.class);

  private final CairnEmbeddingProperties properties;

  // WHY: ZooModel is thread-safe — it holds read-only model weights.
  // We load it once and share it across all threads.
  private ZooModel<String, float[]> model;

  // WHY: Tracks whether the model loaded successfully. Consumers can check health
  // before calling embed(), and health indicators can report status.
  private volatile boolean healthy = false;

  public LocalEmbeddingService(CairnEmbeddingProperties properties) {
    this.properties = properties;
  }

  /**
   * Initializes the DJL model after dependency injection is complete.
   *
   * <p>WHY: Using @PostConstruct instead of constructor initialization because: (1) The
   * CairnEmbeddingProperties must be fully injected first. (2) If model loading fails, the Spring
   * context still starts (we set healthy=false) rather than crashing the entire application.
   */
  @PostConstruct
  void initModel() {
    log.info(
        "Initializing LocalEmbeddingService with model: {} engine: {}",
        properties.getModelUrl(),
        properties.getEngine());
    try {
      Criteria<String, float[]> criteria =
          Criteria.builder()
              .setTypes(String.class, float[].class)
              .optModelUrls(properties.getModelUrl())
              .optEngine(properties.getEngine())
              .build();

      this.model = criteria.loadModel();
      this.healthy = true;
      log.info(
          "LocalEmbeddingService initialized successfully. Dimensions: {}",
          properties.getDimensions());
    } catch (ModelNotFoundException | MalformedModelException | IOException e) {
      log.error("Failed to initialize LocalEmbeddingService — embedding will be unavailable", e);
      this.healthy = false;
      // WHY: We do NOT throw here. The application starts in degraded mode.
      // Callers must check isHealthy() or handle the ModelNotLoadedException.
    }
  }

  /**
   * Converts a text prompt into a 384-dimensional float vector.
   *
   * <p>WHY: Returns float[] instead of List&lt;Double&gt; because: (1) DJL natively produces
   * float[], avoiding a wasteful conversion loop. (2) pgvector and hibernate-vector expect float[].
   * (3) No boxing overhead (float vs Double).
   *
   * @param text The input string to embed. Must not be null or blank.
   * @return A 384-dimensional float array representing the semantic embedding.
   * @throws IllegalArgumentException if text is null or blank.
   * @throws IllegalStateException if the model is not loaded (unhealthy).
   * @throws RuntimeException if the DJL inference fails.
   */
  public float[] embed(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Cannot embed null or blank text");
    }

    if (!healthy) {
      throw new IllegalStateException(
          "LocalEmbeddingService is not healthy — model failed to load. "
              + "Check logs for initialization errors.");
    }

    // WHY: Predictor is NOT thread-safe. It holds mutable inference buffers.
    // Creating a new Predictor per call gives each thread its own private workspace.
    // The cost (~0.1ms overhead) is negligible vs the ~15-20ms embedding computation.
    // try-with-resources ensures the Predictor's buffers are released immediately.
    try (Predictor<String, float[]> predictor = model.newPredictor()) {
      return predictor.predict(text);
    } catch (TranslateException e) {
      log.error("Failed to generate embedding for text: '{}'", text, e);
      throw new RuntimeException("Embedding generation failed", e);
    }
  }

  /**
   * Reports whether the embedding model loaded successfully.
   *
   * <p>WHY: Used by health indicators and callers to check service availability before attempting
   * to embed. Avoids surprise IllegalStateExceptions.
   *
   * @return true if the model is loaded and ready for inference.
   */
  public boolean isHealthy() {
    return healthy;
  }

  /**
   * Returns the configured embedding dimensions.
   *
   * <p>WHY: Consumers (like DomainRouter) need to know the vector size to construct pgvector
   * queries with the correct CAST type.
   *
   * @return The embedding dimension count (default 384).
   */
  public int getDimensions() {
    return properties.getDimensions();
  }

  @PreDestroy
  void close() {
    if (model != null) {
      model.close();
    }
    log.info("LocalEmbeddingService resources closed.");
  }
}
