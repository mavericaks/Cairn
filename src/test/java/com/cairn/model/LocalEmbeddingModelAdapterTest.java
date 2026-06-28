package com.cairn.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

@ExtendWith(MockitoExtension.class)
class LocalEmbeddingModelAdapterTest {

  @Mock private LocalEmbeddingService mockLocalEmbeddingService;

  private LocalEmbeddingModelAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new LocalEmbeddingModelAdapter(mockLocalEmbeddingService);
  }

  @Test
  void embedDocument_success() throws Exception {
    // Arrange
    Document doc = new Document("hello world");
    float[] expectedEmbedding = new float[] {0.1f, 0.2f, 0.3f};
    when(mockLocalEmbeddingService.embed("hello world")).thenReturn(expectedEmbedding);

    // Act
    float[] result = adapter.embed(doc);

    // Assert
    assertThat(result).isEqualTo(expectedEmbedding);
  }

  @Test
  void embedBatch_success() throws Exception {
    // Arrange
    List<String> texts = List.of("hello", "world");
    EmbeddingRequest request = new EmbeddingRequest(texts, null);

    when(mockLocalEmbeddingService.embed("hello")).thenReturn(new float[] {0.1f});
    when(mockLocalEmbeddingService.embed("world")).thenReturn(new float[] {0.2f});

    // Act
    EmbeddingResponse response = adapter.call(request);

    // Assert
    assertThat(response.getResults()).hasSize(2);
    assertThat(response.getResults().get(0).getOutput()).containsExactly(0.1f);
    assertThat(response.getResults().get(1).getOutput()).containsExactly(0.2f);
  }
}
