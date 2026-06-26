package com.cairn.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

class OllamaHealthIndicatorTest {

  private OllamaModelManager modelManager;
  private CairnOllamaProperties properties;
  private OllamaHealthIndicator healthIndicator;

  @BeforeEach
  void setUp() {
    modelManager = mock(OllamaModelManager.class);
    properties = new CairnOllamaProperties();
    properties.setBaseUrl("http://localhost:11434");
    properties.setDefaultModel("llama3.2");

    healthIndicator = new OllamaHealthIndicator(modelManager, properties);
  }

  @Test
  void shouldReturnDownWhenOllamaNotRunning() {
    when(modelManager.isOllamaRunning()).thenReturn(false);

    Health health = healthIndicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  void shouldReturnDegradedWhenModelNotAvailable() {
    when(modelManager.isOllamaRunning()).thenReturn(true);
    when(modelManager.isModelAvailable()).thenReturn(false);

    Health health = healthIndicator.health();

    // Using custom DEGRADED status since Spring Boot doesn't have it by default
    assertThat(health.getStatus().getCode()).isEqualTo("DEGRADED");
  }

  @Test
  void shouldReturnUpWhenAllHealthy() {
    when(modelManager.isOllamaRunning()).thenReturn(true);
    when(modelManager.isModelAvailable()).thenReturn(true);

    Health health = healthIndicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
  }
}
