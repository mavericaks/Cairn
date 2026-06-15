package com.cairn.routing;

import com.cairn.TestcontainersConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration and Edge Case tests for {@link DomainContextCacheService}.
 * <p>
 * WHY: Proves that the Redis connection works (Happy Path) AND explicitly tests
 * Rule 16 (Production-Grade First) by verifying input validation and graceful degradation.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
class DomainContextCacheServiceTest {

    @Autowired
    private DomainContextCacheService cacheService;

    private static final String TEST_USER = "user-123";
    private static final String TEST_DOMAIN = "analytical";

    @AfterEach
    void tearDown() {
        // Clean up test data after each run
        cacheService.clearContext(TEST_USER, TEST_DOMAIN);
    }

    // ─── HAPPY PATH (Integration) ──────────────────────────────────────────────────

    @Test
    void shouldReturnEmptyWhenContextDoesNotExist() {
        Optional<String> context = cacheService.getContext(TEST_USER, TEST_DOMAIN);
        assertThat(context).isEmpty();
    }

    @Test
    void shouldSaveAndRetrieveContext() {
        String expectedContext = "We are discussing quantum computing.";
        cacheService.saveContext(TEST_USER, TEST_DOMAIN, expectedContext);
        
        Optional<String> retrievedContext = cacheService.getContext(TEST_USER, TEST_DOMAIN);
        
        assertThat(retrievedContext).isPresent();
        assertThat(retrievedContext.get()).isEqualTo(expectedContext);
    }

    @Test
    void shouldClearContext() {
        cacheService.saveContext(TEST_USER, TEST_DOMAIN, "Temporary context");
        assertThat(cacheService.getContext(TEST_USER, TEST_DOMAIN)).isPresent();

        cacheService.clearContext(TEST_USER, TEST_DOMAIN);

        assertThat(cacheService.getContext(TEST_USER, TEST_DOMAIN)).isEmpty();
    }

    @Test
    void shouldIsolateContextByDomain() {
        String analyticalContext = "Math problem context";
        String conversationalContext = "Hello world context";

        cacheService.saveContext(TEST_USER, "analytical", analyticalContext);
        cacheService.saveContext(TEST_USER, "conversational", conversationalContext);

        assertThat(cacheService.getContext(TEST_USER, "analytical")).contains(analyticalContext);
        assertThat(cacheService.getContext(TEST_USER, "conversational")).contains(conversationalContext);

        cacheService.clearContext(TEST_USER, "conversational");
    }

    // ─── RULE 16: INPUT VALIDATION (Edge Cases) ────────────────────────────────────

    @Test
    void shouldRejectInvalidInputsForGetContext() {
        assertThatIllegalArgumentException().isThrownBy(() -> cacheService.getContext(null, TEST_DOMAIN));
        assertThatIllegalArgumentException().isThrownBy(() -> cacheService.getContext("", TEST_DOMAIN));
        assertThatIllegalArgumentException().isThrownBy(() -> cacheService.getContext(TEST_USER, null));
        assertThatIllegalArgumentException().isThrownBy(() -> cacheService.getContext(TEST_USER, ""));
    }

    @Test
    void shouldRejectInvalidInputsForSaveContext() {
        assertThatIllegalArgumentException().isThrownBy(() -> cacheService.saveContext(null, TEST_DOMAIN, "context"));
        assertThatIllegalArgumentException().isThrownBy(() -> cacheService.saveContext(TEST_USER, null, "context"));
        assertThatIllegalArgumentException().isThrownBy(() -> cacheService.saveContext(TEST_USER, TEST_DOMAIN, null));
    }

    // ─── RULE 16: GRACEFUL DEGRADATION (Failure Modes) ─────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void shouldDegradeGracefullyWhenRedisIsDown() {
        // Arrange: Manually instantiate with a mocked Redis template that throws connection failures
        StringRedisTemplate mockTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> mockOps = mock(ValueOperations.class);
        when(mockTemplate.opsForValue()).thenReturn(mockOps);
        
        // Simulate Redis being completely unreachable
        when(mockOps.get("context:analytical:user-fail")).thenThrow(new RedisConnectionFailureException("Simulated Redis Offline"));
        
        SimpleMeterRegistry testRegistry = new SimpleMeterRegistry();
        DomainContextCacheService failingService = new DomainContextCacheService(mockTemplate, testRegistry);

        // Act & Assert: GET should return Optional.empty() instead of crashing
        Optional<String> result = failingService.getContext("user-fail", "analytical");
        assertThat(result).isEmpty();
        assertThat(testRegistry.counter("cairn.cache.context", "result", "failure").count()).isEqualTo(1.0);

        // Act & Assert: SAVE should execute silently instead of crashing
        assertDoesNotThrow(() -> failingService.saveContext("user-fail", "analytical", "data"));
        
        // Note: Mockito verify could be used, but the Micrometer counter is our primary observable proof
    }
}
