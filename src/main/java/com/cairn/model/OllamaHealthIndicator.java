package com.cairn.model;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * WHY: Custom health indicator for Spring Actuator. Reports the status of the local Ollama engine
 * to /actuator/health so orchestrators (like Kubernetes or Railway) know if we are degraded.
 */
@Component
public class OllamaHealthIndicator implements HealthIndicator {

  private final OllamaModelManager modelManager;
  private final CairnOllamaProperties properties;

  public OllamaHealthIndicator(OllamaModelManager modelManager, CairnOllamaProperties properties) {
    this.modelManager = modelManager;
    this.properties = properties;
  }

  @Override
  public Health health() {
    if (!modelManager.isOllamaRunning()) {
      return Health.down()
          .withDetail("error", "Ollama is not reachable at " + properties.getBaseUrl())
          .build();
    }

    if (!modelManager.isModelAvailable()) {
      return Health.status("DEGRADED")
          .withDetail(
              "warning",
              "Ollama is running, but model '" + properties.getDefaultModel() + "' is not pulled.")
          .build();
    }

    return Health.up()
        .withDetail("model", properties.getDefaultModel())
        .withDetail("url", properties.getBaseUrl())
        .build();
  }
}
