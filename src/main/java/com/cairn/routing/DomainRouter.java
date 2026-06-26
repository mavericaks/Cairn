package com.cairn.routing;

import com.cairn.model.exception.DomainNotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The core semantic routing engine.
 *
 * <p>WHY: This service bridges the local embedding model and the PostgreSQL vector database to
 * determine user intent without calling an external LLM. It includes a native pgvector query for
 * extreme performance and an in-memory fallback for graceful degradation (SDE standard #10 & #13).
 */
@Service
public class DomainRouter {

  private static final Logger log = LoggerFactory.getLogger(DomainRouter.class);

  private final LocalEmbeddingService embeddingService;
  private final EntityManager entityManager;
  private final DomainRepository domainRepository;

  public DomainRouter(
      LocalEmbeddingService embeddingService,
      EntityManager entityManager,
      DomainRepository domainRepository) {
    this.embeddingService = embeddingService;
    this.entityManager = entityManager;
    this.domainRepository = domainRepository;
  }

  /**
   * Routes the user's text query to the most semantically similar domain.
   *
   * @param userQuery The raw text from the user.
   * @return A RoutingResult containing the chosen domain and confidence score.
   * @throws DomainNotFoundException If no active domains exist.
   */
  @Transactional(readOnly = true)
  public RoutingResult route(String userQuery) {
    long start = System.currentTimeMillis();

    // 1. Embed the query locally (fast, no network)
    float[] queryVector = embeddingService.embed(userQuery);

    try {
      // 2. Try native PostgreSQL vector search (fastest, relies on pgvector extension)
      RoutingResult result = executeNativeSearch(queryVector);
      if (result != null) {
        long latency = System.currentTimeMillis() - start;
        log.info(
            "Routed to '{}' with score {} in {}ms (native)",
            result.domainName(),
            result.score(),
            latency);
        return new RoutingResult(result.domainName(), result.score(), latency);
      }
    } catch (Exception e) {
      log.error(
          "Native vector search failed, falling back to in-memory search. Error: {}",
          e.getMessage(),
          e);
    }

    // 3. Graceful degradation: In-memory Java search
    RoutingResult fallbackResult = executeInMemorySearch(queryVector);
    long latency = System.currentTimeMillis() - start;
    log.info(
        "Routed to '{}' with score {} in {}ms (fallback)",
        fallbackResult.domainName(),
        fallbackResult.score(),
        latency);
    return new RoutingResult(fallbackResult.domainName(), fallbackResult.score(), latency);
  }

  /**
   * WHY: Executes a native SQL query bypassing JPA ORM. JPA does not support the pgvector {@code
   * <=>} operator, so this is necessary for utilizing the HNSW index on the database side.
   */
  @SuppressWarnings("unchecked")
  private RoutingResult executeNativeSearch(float[] queryVector) {
    // We must pass the float array as a PostgreSQL array format string '{0.1, 0.2, ...}'
    // because standard JDBC doesn't know how to bind float[] to vector types natively easily.
    String vectorString = floatArrayToVectorString(queryVector);

    String sql =
        """
        SELECT d.name, 1 - (de.embedding <=> CAST(:queryVector AS vector)) AS score
        FROM domain_examples de
        JOIN domains d ON de.domain_id = d.id
        WHERE d.active = true
        ORDER BY de.embedding <=> CAST(:queryVector AS vector)
        LIMIT 1
        """;

    Query query = entityManager.createNativeQuery(sql);
    query.setParameter("queryVector", vectorString);

    List<Object[]> results = query.getResultList();
    if (results.isEmpty()) {
      return null;
    }

    Object[] row = results.get(0);
    String domainName = (String) row[0];
    double score = ((Number) row[1]).doubleValue();

    return new RoutingResult(domainName, score, 0); // Latency injected later
  }

  /**
   * WHY: Fallback mechanism in case the DB doesn't support pgvector or the native query fails.
   * Fetches all active domains and their examples, and manually computes cosine similarity in the
   * JVM.
   */
  private RoutingResult executeInMemorySearch(float[] queryVector) {
    List<Domain> activeDomains =
        domainRepository.findAll().stream().filter(Domain::isActive).toList();

    if (activeDomains.isEmpty()) {
      throw new DomainNotFoundException("No active domains found for routing.");
    }

    String bestDomain = null;
    double bestScore = -1.0; // Cosine similarity ranges from -1.0 to 1.0

    for (Domain domain : activeDomains) {
      for (DomainExample example : domain.getExamples()) {
        double score = cosineSimilarity(queryVector, example.getEmbedding());
        if (score > bestScore) {
          bestScore = score;
          bestDomain = domain.getName();
        }
      }
    }

    if (bestDomain == null) {
      throw new DomainNotFoundException("No examples found to route against.");
    }

    return new RoutingResult(bestDomain, bestScore, 0); // Latency injected later
  }

  /**
   * Calculates cosine similarity mathematically.
   *
   * <p>WHY: Formula is (A dot B) / (||A|| * ||B||). Necessary for the in-memory fallback.
   */
  private double cosineSimilarity(float[] vectorA, float[] vectorB) {
    double dotProduct = 0.0;
    double normA = 0.0;
    double normB = 0.0;
    for (int i = 0; i < vectorA.length; i++) {
      dotProduct += vectorA[i] * vectorB[i];
      normA += vectorA[i] * vectorA[i];
      normB += vectorB[i] * vectorB[i];
    }
    if (normA == 0.0 || normB == 0.0) {
      return 0.0;
    }
    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
  }

  /** Helper to convert float[] to Postgres vector string notation: '[0.1, 0.2, ...]' */
  private String floatArrayToVectorString(float[] array) {
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < array.length; i++) {
      sb.append(array[i]);
      if (i < array.length - 1) {
        sb.append(",");
      }
    }
    sb.append("]");
    return sb.toString();
  }
}
