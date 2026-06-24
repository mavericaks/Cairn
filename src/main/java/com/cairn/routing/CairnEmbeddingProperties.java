package com.cairn.routing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe configuration properties for the local embedding model.
 *
 * <p>WHY: Externalizing model URL, engine, and dimension count into a @ConfigurationProperties
 * class gives us type safety, IDE autocompletion, validation, and profile-specific overrides — all
 * impossible with hardcoded strings in Java code. (SDE Standard #3)
 *
 * <p>Binds to the {@code cairn.embedding.*} YAML subtree. Spring automatically maps kebab-case
 * (model-url) to camelCase (modelUrl).
 */
@Validated
@ConfigurationProperties(prefix = "cairn.embedding")
public class CairnEmbeddingProperties {

  /**
   * The DJL model URL. Points to the HuggingFace model to download. WHY: Externalized so we can
   * swap models (e.g. to all-MiniLM-L12-v2) without code changes.
   */
  @NotBlank
  private String modelUrl =
      "djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2";

  /**
   * The DJL engine name. PyTorch is the default for HuggingFace models. WHY: Externalized because
   * ONNX Runtime is a valid alternative engine with different performance characteristics.
   */
  @NotBlank private String engine = "PyTorch";

  /**
   * The dimensionality of the embedding vectors produced by the model. WHY: This must match the
   * pgvector column definition (vector(384)). A mismatch would cause silent data corruption during
   * similarity search.
   */
  @Positive private int dimensions = 384;

  public String getModelUrl() {
    return modelUrl;
  }

  public void setModelUrl(String modelUrl) {
    this.modelUrl = modelUrl;
  }

  public String getEngine() {
    return engine;
  }

  public void setEngine(String engine) {
    this.engine = engine;
  }

  public int getDimensions() {
    return dimensions;
  }

  public void setDimensions(int dimensions) {
    this.dimensions = dimensions;
  }
}
