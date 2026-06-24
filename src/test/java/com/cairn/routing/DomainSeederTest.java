package com.cairn.routing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

/**
 * Unit tests for DomainSeeder to ensure it accurately seeds missing domains and respects
 * idempotency by not duplicating existing ones.
 *
 * <p>WHY: Tests now verify: (1) all 6 domains seeded when DB is empty, (2) skipping when domains
 * exist, (3) graceful degradation when embedding fails for one domain — the other 5 should still be
 * seeded.
 */
@ExtendWith(MockitoExtension.class)
class DomainSeederTest {

  @Mock private DomainRepository domainRepository;

  @Mock private LocalEmbeddingService embeddingService;

  private DomainSeeder domainSeeder;

  @BeforeEach
  void setUp() {
    domainSeeder = new DomainSeeder(domainRepository, embeddingService);
  }

  @Test
  void shouldSeedAllDomainsWhenDatabaseIsEmpty() {
    // Arrange
    when(domainRepository.existsByName(anyString())).thenReturn(false);

    // WHY: embed() now returns float[] directly — no List<Double> conversion.
    float[] mockEmbedding = new float[] {0.1f, 0.2f, 0.3f};
    when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);

    // Act
    domainSeeder.run(new DefaultApplicationArguments());

    // Assert
    // WHY: Verifies that 6 foundational domains were checked and saved
    verify(domainRepository, times(6)).existsByName(anyString());
    verify(embeddingService, times(6)).embed(anyString());
    verify(domainRepository, times(6)).save(any(Domain.class));
  }

  @Test
  void shouldSkipSeedingWhenDomainsAlreadyExist() {
    // Arrange
    // WHY: Simulate that all domains are already in the DB (idempotency check)
    when(domainRepository.existsByName(anyString())).thenReturn(true);

    // Act
    domainSeeder.run(new DefaultApplicationArguments());

    // Assert
    verify(domainRepository, times(6)).existsByName(anyString());

    // WHY: If they exist, we shouldn't waste CPU computing embeddings or do DB writes
    verify(embeddingService, never()).embed(anyString());
    verify(domainRepository, never()).save(any(Domain.class));
  }

  @Test
  void shouldContinueSeedingWhenOneEmbeddingFails() {
    // Arrange
    when(domainRepository.existsByName(anyString())).thenReturn(false);

    // WHY: First call succeeds, second call throws, rest succeed.
    // This proves per-domain try-catch prevents one failure from aborting all 6.
    when(embeddingService.embed(anyString()))
        .thenReturn(new float[] {0.1f, 0.2f, 0.3f}) // system - success
        .thenThrow(new RuntimeException("Simulated embedding failure")) // conversational - fail
        .thenReturn(new float[] {0.1f, 0.2f, 0.3f}) // discovery - success
        .thenReturn(new float[] {0.1f, 0.2f, 0.3f}) // execution - success
        .thenReturn(new float[] {0.1f, 0.2f, 0.3f}) // generative - success
        .thenReturn(new float[] {0.1f, 0.2f, 0.3f}); // analytical - success

    // Act
    domainSeeder.run(new DefaultApplicationArguments());

    // Assert
    // WHY: All 6 domains should be attempted (embed called 6 times),
    // but only 5 should be saved (one failed).
    verify(embeddingService, times(6)).embed(anyString());
    verify(domainRepository, times(5)).save(any(Domain.class));
  }
}
