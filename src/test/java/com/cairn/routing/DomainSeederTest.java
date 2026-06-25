package com.cairn.routing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
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
    when(domainRepository.findByName(anyString())).thenReturn(Optional.empty());

    // WHY: embed() now returns float[] directly — no List<Double> conversion.
    float[] mockEmbedding = new float[] {0.1f, 0.2f, 0.3f};
    when(embeddingService.embed(anyString())).thenReturn(mockEmbedding);

    // Act
    domainSeeder.run(new DefaultApplicationArguments());

    // Assert
    // WHY: Verifies that 6 foundational domains were checked and saved.
    // 66 embeddings total (6 domains + 60 examples).
    verify(domainRepository, times(6)).findByName(anyString());
    verify(embeddingService, times(66)).embed(anyString());
    verify(domainRepository, times(6)).save(any(Domain.class));
  }

  @Test
  void shouldSkipSeedingWhenDomainsAlreadyExist() {
    // Arrange
    // WHY: Simulate that all domains are already in the DB and have examples
    Domain existingDomain = new Domain("system", "desc", new float[] {0.1f});
    existingDomain.addExample(new DomainExample(existingDomain, "ex", new float[] {0.1f}));
    when(domainRepository.findByName(anyString())).thenReturn(Optional.of(existingDomain));

    // Act
    domainSeeder.run(new DefaultApplicationArguments());

    // Assert
    verify(domainRepository, times(6)).findByName(anyString());

    // WHY: If they exist with examples, we shouldn't waste CPU computing embeddings or do DB writes
    verify(embeddingService, never()).embed(anyString());
    verify(domainRepository, never()).save(any(Domain.class));
  }

  @Test
  void shouldContinueSeedingWhenOneEmbeddingFails() {
    // Arrange
    when(domainRepository.findByName(anyString())).thenReturn(Optional.empty());

    // WHY: First domain (11 embeds) succeeds, second domain (1st embed) throws, rest succeed.
    // This proves per-domain try-catch prevents one failure from aborting all 6.
    when(embeddingService.embed(anyString()))
        // system (1 desc + 10 examples)
        .thenReturn(new float[] {0.1f})
        .thenReturn(new float[] {0.1f})
        .thenReturn(new float[] {0.1f})
        .thenReturn(new float[] {0.1f})
        .thenReturn(new float[] {0.1f})
        .thenReturn(new float[] {0.1f})
        .thenReturn(new float[] {0.1f})
        .thenReturn(new float[] {0.1f})
        .thenReturn(new float[] {0.1f})
        .thenReturn(new float[] {0.1f})
        .thenReturn(new float[] {0.1f})
        // conversational - fail on description
        .thenThrow(new RuntimeException("Simulated embedding failure"))
        // the remaining 4 domains (44 embeds total) will succeed, returning default mock
        .thenReturn(new float[] {0.1f});

    // Act
    domainSeeder.run(new DefaultApplicationArguments());

    // Assert
    // WHY: 5 domains fully succeed (55 embeds) + 1 domain fails on 1st embed = 56 embeds
    verify(embeddingService, times(56)).embed(anyString());
    verify(domainRepository, times(5)).save(any(Domain.class));
  }
}
