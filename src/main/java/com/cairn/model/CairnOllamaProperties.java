package com.cairn.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * WHY: SDE Standard #3. Type-safe configuration properties for Ollama integration. Enables IDE
 * auto-completion and startup validation.
 */
@Validated
@ConfigurationProperties(prefix = "cairn.ollama")
public class CairnOllamaProperties {

  @NotBlank private String baseUrl = "http://localhost:11434";

  @NotBlank private String defaultModel = "llama3.2";

  @Positive private int timeoutSeconds = 60;

  @Positive private int contextWindow = 4096;

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getDefaultModel() {
    return defaultModel;
  }

  public void setDefaultModel(String defaultModel) {
    this.defaultModel = defaultModel;
  }

  public int getTimeoutSeconds() {
    return timeoutSeconds;
  }

  public void setTimeoutSeconds(int timeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds;
  }

  public int getContextWindow() {
    return contextWindow;
  }

  public void setContextWindow(int contextWindow) {
    this.contextWindow = contextWindow;
  }
}
