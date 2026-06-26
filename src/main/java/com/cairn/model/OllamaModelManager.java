package com.cairn.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * WHY: Manages the lifecycle and state of the local Ollama instance. Exposes methods to check if
 * models are loaded, load them, and manage memory constraints.
 */
@Service
public class OllamaModelManager {

  private static final Logger log = LoggerFactory.getLogger(OllamaModelManager.class);

  private final CairnOllamaProperties properties;
  private final RestClient restClient;

  public OllamaModelManager(
      CairnOllamaProperties properties, RestClient.Builder restClientBuilder) {
    this.properties = properties;
    this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl()).build();
  }

  /** Checks if Ollama is running and responsive. */
  public boolean isOllamaRunning() {
    try {
      String response = restClient.get().uri("/").retrieve().body(String.class);
      return response != null && response.contains("Ollama is running");
    } catch (RestClientException e) {
      log.debug("Ollama is not running at {}: {}", properties.getBaseUrl(), e.getMessage());
      return false;
    }
  }

  /** Checks if the required model is available in the local registry. */
  public boolean isModelAvailable() {
    try {
      String response = restClient.get().uri("/api/tags").retrieve().body(String.class);
      return response != null && response.contains(properties.getDefaultModel());
    } catch (RestClientException e) {
      log.error("Failed to fetch tags from Ollama", e);
      return false;
    }
  }
}
