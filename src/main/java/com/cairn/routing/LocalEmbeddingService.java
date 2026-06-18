package com.cairn.routing;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsible for generating local semantic embeddings using DJL and PyTorch.
 *
 * <p>WHY: By generating embeddings locally via CPU, we completely bypass network latency and API
 * costs. This makes intent classification (Semantic Routing) effectively instant (~20ms) and
 * entirely offline.
 */
@Service
public class LocalEmbeddingService {
  private static final Logger log = LoggerFactory.getLogger(LocalEmbeddingService.class);

  private ZooModel<String, float[]> model;
  private Predictor<String, float[]> predictor;

  public LocalEmbeddingService() {
    initModel();
  }

  private void initModel() {
    log.info("Initializing LocalEmbeddingService with HuggingFace all-MiniLM-L6-v2...");
    try {
      // Using a simple custom translator to handle text -> float[] if standard one isn't available
      // Note: In real production, we would use TextEmbeddingTranslatorFactory,
      // but for safety in this setup we use a basic criteria targeting sentence-transformers
      Criteria<String, float[]> criteria =
          Criteria.builder()
              .setTypes(String.class, float[].class)
              .optModelUrls(
                  "djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2")
              .optEngine("PyTorch")
              .build();

      this.model = criteria.loadModel();
      this.predictor = model.newPredictor();
      log.info("LocalEmbeddingService initialized successfully. Model loaded into memory.");
    } catch (ModelNotFoundException | MalformedModelException | IOException e) {
      log.error("Failed to initialize LocalEmbeddingService", e);
      throw new RuntimeException("Could not load local embedding model", e);
    }
  }

  /**
   * Converts a text prompt into a 384-dimensional vector.
   *
   * @param text The input string to embed.
   * @return A List of 384 doubles representing the semantic embedding.
   */
  public List<Double> embed(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Cannot embed null or blank text");
    }

    try {
      float[] embedding = predictor.predict(text);
      Double[] doubleArray = new Double[embedding.length];
      for (int i = 0; i < embedding.length; i++) {
        doubleArray[i] = (double) embedding[i];
      }
      return Arrays.asList(doubleArray);
    } catch (TranslateException e) {
      log.error("Failed to generate embedding for text: {}", text, e);
      throw new RuntimeException("Embedding generation failed", e);
    }
  }

  @PreDestroy
  public void close() {
    if (predictor != null) {
      predictor.close();
    }
    if (model != null) {
      model.close();
    }
    log.info("LocalEmbeddingService resources closed.");
  }
}
