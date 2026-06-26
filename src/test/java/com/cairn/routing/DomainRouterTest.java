package com.cairn.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cairn.TestcontainersConfig;
import com.cairn.model.exception.DomainNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for DomainRouter.
 *
 * <p>WHY: End-to-end test verifying that the router successfully connects to PostgreSQL via
 * Testcontainers, computes the embedding via DJL locally, performs the pgvector cosine similarity
 * search natively, and correctly routes real text inputs to the expected domains seeded by the
 * ApplicationRunner.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test") // Ensures consistent logging/config
class DomainRouterTest {

  @Autowired private DomainRouter domainRouter;

  @Autowired private DomainRepository domainRepository;

  @Test
  void shouldRouteToExecutionDomainForRestartCommand() {
    // Act
    RoutingResult result = domainRouter.route("Restart the production server please.");

    // Assert
    assertThat(result.domainName()).isEqualTo("execution");
    assertThat(result.score()).isGreaterThan(0.5); // Reasonably confident match
    assertThat(result.latencyMs()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void shouldRouteToDiscoveryDomainForSearchQuery() {
    // Act
    RoutingResult result = domainRouter.route("Where is the Q3 financial report?");

    // Assert
    assertThat(result.domainName()).isEqualTo("discovery");
    assertThat(result.score()).isGreaterThan(0.5);
  }

  @Test
  void shouldRouteToGenerativeDomainForCodeRequest() {
    // Act
    RoutingResult result = domainRouter.route("Write a python script to scrape data.");

    // Assert
    assertThat(result.domainName()).isEqualTo("generative");
    assertThat(result.score()).isGreaterThan(0.5);
  }

  @Test
  void shouldRouteToConversationalDomainForGreeting() {
    // Act
    RoutingResult result = domainRouter.route("Hey! How is your day going?");

    // Assert
    assertThat(result.domainName()).isEqualTo("conversational");
    assertThat(result.score()).isGreaterThan(0.5);
  }

  @Test
  void shouldThrowExceptionWhenNoActiveDomainsExist() {
    // Arrange: Soft delete all domains
    domainRepository
        .findAll()
        .forEach(
            d -> {
              d.setActive(false);
              domainRepository.save(d);
            });

    // Act & Assert
    assertThrows(DomainNotFoundException.class, () -> domainRouter.route("Hello?"));

    // Cleanup: Restore domains for other tests
    domainRepository
        .findAll()
        .forEach(
            d -> {
              d.setActive(true);
              domainRepository.save(d);
            });
  }
}
