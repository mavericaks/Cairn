package com.cairn.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.cairn.TestcontainersConfig;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * End-to-end integration test proving the full Epic 2 routing pipeline works.
 *
 * <p>WHY: This is the Definition of Done for Epic 2. It verifies that a user query can be embedded
 * locally via DJL, routed natively via pgvector HNSW search, and the resulting domain can be
 * successfully cached in Redis for the upcoming Epic 3 agents to use.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
class SemanticKernelTests {

  @Autowired private DomainRouter domainRouter;

  @Autowired private DomainContextCacheService cacheService;

  @Test
  void shouldRouteQueryAndCacheResultEndToEnd() {
    // 1. Setup a fake user session
    UUID userId = UUID.randomUUID();
    String userQuery = "Can you help me reset my account password?";

    // 2. Execute the semantic routing pipeline (DJL -> Postgres pgvector)
    RoutingResult result = domainRouter.route(userQuery);

    // 3. Verify the vector math accurately classified the intent
    assertThat(result).isNotNull();
    assertThat(result.domainName()).isEqualTo("execution"); // Password reset is an execution task
    assertThat(result.score()).isGreaterThan(0.5);

    // 4. Cache some context in Redis for the resulting domain
    cacheService.saveContext(userId.toString(), result.domainName(), "User intent verified");

    // 5. Retrieve the context from Redis to prove it persisted across the network boundary
    Optional<String> cachedContext =
        cacheService.getContext(userId.toString(), result.domainName());
    assertThat(cachedContext).isPresent().contains("User intent verified");

    // 6. Test a different domain to prove it's not a fluke
    String analyticalQuery = "Calculate the standard deviation of our Q4 revenue.";
    RoutingResult analyticalResult = domainRouter.route(analyticalQuery);

    assertThat(analyticalResult.domainName()).isEqualTo("analytical");
    cacheService.saveContext(userId.toString(), analyticalResult.domainName(), "Math context");

    Optional<String> newCachedContext =
        cacheService.getContext(userId.toString(), analyticalResult.domainName());
    assertThat(newCachedContext).isPresent().contains("Math context");
  }
}
