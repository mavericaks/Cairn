package com.cairn.routing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

/**
 * Unit tests for DomainSeeder to ensure it accurately seeds missing domains and respects
 * idempotency by not duplicating existing ones.
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

    List<Double> mockEmbedding = Arrays.asList(0.1, 0.2, 0.3);
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
}
