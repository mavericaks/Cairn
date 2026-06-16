package com.cairn.routing;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Service responsible for managing domain-specific conversational context in Redis.
 *
 * <p>WHY: This acts as the external domain context cache between intent selection and LLM execution
 * (ADR-004). It isolates context per user and per domain.
 *
 * <p>RULE 16 ENFORCED: This service implements graceful degradation. If Redis goes offline, the
 * application will survive (treating queries as cache misses) rather than crashing the entire
 * semantic routing pipeline. It also records Micrometer metrics.
 */
@Service
public class DomainContextCacheService {

  private static final Logger log = LoggerFactory.getLogger(DomainContextCacheService.class);

  // WHY: 1 hour TTL protects the strict 30MB free-tier Redis limit.
  private static final Duration TTL = Duration.ofHours(1);

  private final StringRedisTemplate redisTemplate;
  private final MeterRegistry meterRegistry;

  public DomainContextCacheService(StringRedisTemplate redisTemplate, MeterRegistry meterRegistry) {
    this.redisTemplate = redisTemplate;
    this.meterRegistry = meterRegistry;
  }

  /**
   * Retrieves the cached context for a specific user in a specific domain.
   *
   * @param userId The unique identifier of the user (must not be null/empty).
   * @param domain The semantic domain (must not be null/empty).
   * @return An Optional containing the context if it exists, empty otherwise. Returns empty if
   *     Redis is unreachable, allowing the system to degrade gracefully.
   */
  public Optional<String> getContext(String userId, String domain) {
    Assert.hasText(userId, "userId must not be null or empty");
    Assert.hasText(domain, "domain must not be null or empty");

    String key = buildKey(userId, domain);

    try {
      String context = redisTemplate.opsForValue().get(key);

      if (context != null) {
        meterRegistry.counter("cairn.cache.context", "result", "hit", "domain", domain).increment();
        log.debug("Context cache HIT for user [{}] in domain [{}]", userId, domain);
        return Optional.of(context);
      } else {
        meterRegistry
            .counter("cairn.cache.context", "result", "miss", "domain", domain)
            .increment();
        log.debug("Context cache MISS for user [{}] in domain [{}]", userId, domain);
        return Optional.empty();
      }
    } catch (RedisConnectionFailureException | QueryTimeoutException e) {
      meterRegistry.counter("cairn.cache.context", "result", "failure").increment();
      log.error(
          "Redis infrastructure failure while fetching context. Degrading gracefully to cache MISS. Key: [{}], Reason: {}",
          key,
          e.getMessage());
      return Optional.empty();
    } catch (Exception e) {
      meterRegistry.counter("cairn.cache.context", "result", "error").increment();
      log.error(
          "Unexpected error accessing Redis context cache. Degrading to MISS. Key: [{}], Error: {}",
          key,
          e.getMessage(),
          e);
      return Optional.empty();
    }
  }

  /**
   * Saves the provided context for a specific user and domain with a 1-hour TTL.
   *
   * @param userId The unique identifier of the user (must not be null/empty).
   * @param domain The semantic domain (must not be null/empty).
   * @param context The text context to cache (must not be null).
   */
  public void saveContext(String userId, String domain, String context) {
    Assert.hasText(userId, "userId must not be null or empty");
    Assert.hasText(domain, "domain must not be null or empty");
    Assert.notNull(context, "context must not be null");

    String key = buildKey(userId, domain);

    try {
      redisTemplate.opsForValue().set(key, context, TTL);
      meterRegistry.counter("cairn.cache.save", "status", "success").increment();
      log.info(
          "Saved context for user [{}] in domain [{}] with TTL {} hours",
          userId,
          domain,
          TTL.toHours());
    } catch (RedisConnectionFailureException | QueryTimeoutException e) {
      meterRegistry.counter("cairn.cache.save", "status", "failure").increment();
      log.error(
          "Redis infrastructure failure while saving context. Proceeding without caching. Key: [{}], Reason: {}",
          key,
          e.getMessage());
      // WHY: Do not throw. Saving cache is not a critical path operation that should crash the user
      // request.
    } catch (Exception e) {
      meterRegistry.counter("cairn.cache.save", "status", "error").increment();
      log.error(
          "Unexpected error saving to Redis context cache. Proceeding without caching. Key: [{}], Error: {}",
          key,
          e.getMessage(),
          e);
    }
  }

  /**
   * Clears the context for a specific user in a specific domain.
   *
   * @param userId The unique identifier of the user (must not be null/empty).
   * @param domain The semantic domain (must not be null/empty).
   */
  public void clearContext(String userId, String domain) {
    Assert.hasText(userId, "userId must not be null or empty");
    Assert.hasText(domain, "domain must not be null or empty");

    String key = buildKey(userId, domain);

    try {
      redisTemplate.delete(key);
      meterRegistry.counter("cairn.cache.clear", "status", "success").increment();
      log.info("Cleared context for user [{}] in domain [{}]", userId, domain);
    } catch (RedisConnectionFailureException | QueryTimeoutException e) {
      meterRegistry.counter("cairn.cache.clear", "status", "failure").increment();
      log.error(
          "Redis infrastructure failure while clearing context. Key: [{}], Reason: {}",
          key,
          e.getMessage());
    } catch (Exception e) {
      meterRegistry.counter("cairn.cache.clear", "status", "error").increment();
      log.error(
          "Unexpected error clearing Redis context cache. Key: [{}], Error: {}",
          key,
          e.getMessage(),
          e);
    }
  }

  // WHY: Standardized key format prevents collisions between user IDs and domains.
  private String buildKey(String userId, String domain) {
    return "context:" + domain + ":" + userId;
  }
}
