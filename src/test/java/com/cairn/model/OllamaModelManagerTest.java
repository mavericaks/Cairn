package com.cairn.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OllamaModelManagerTest {

  private OllamaModelManager modelManager;
  private MockRestServiceServer mockServer;

  @BeforeEach
  void setUp() {
    CairnOllamaProperties properties = new CairnOllamaProperties();
    properties.setBaseUrl("http://localhost:11434");
    properties.setDefaultModel("llama3.2");

    RestClient.Builder builder = RestClient.builder();
    this.mockServer = MockRestServiceServer.bindTo(builder).build();
    this.modelManager = new OllamaModelManager(properties, builder);
  }

  @Test
  void shouldReturnTrueWhenOllamaIsRunning() {
    mockServer
        .expect(requestTo("http://localhost:11434/"))
        .andRespond(withSuccess("Ollama is running", MediaType.TEXT_PLAIN));

    assertThat(modelManager.isOllamaRunning()).isTrue();
  }

  @Test
  void shouldReturnTrueWhenModelIsAvailable() {
    mockServer
        .expect(requestTo("http://localhost:11434/api/tags"))
        .andRespond(
            withSuccess(
                "{\"models\": [{\"name\": \"llama3.2:latest\"}]}", MediaType.APPLICATION_JSON));

    assertThat(modelManager.isModelAvailable()).isTrue();
  }
}
