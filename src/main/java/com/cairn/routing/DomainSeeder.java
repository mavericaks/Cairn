package com.cairn.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes the database with the 6 foundational domains on startup.
 *
 * <p>WHY: The domains table requires pre-calculated 384-dim embeddings. Instead of hardcoding
 * massive floating point arrays in Flyway or Java, we dynamically embed the text descriptions on
 * startup using the local DJL engine and save them to PostgreSQL (ADR-011).
 *
 * <p>Each domain is seeded independently with its own try-catch so that one embedding failure does
 * not prevent the other 5 domains from being seeded.
 */
@Component
public class DomainSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(DomainSeeder.class);

  private final DomainRepository domainRepository;
  private final LocalEmbeddingService embeddingService;

  public DomainSeeder(DomainRepository domainRepository, LocalEmbeddingService embeddingService) {
    this.domainRepository = domainRepository;
    this.embeddingService = embeddingService;
  }

  @Override
  public void run(ApplicationArguments args) {
    log.info("Checking and seeding foundational semantic domains...");

    // WHY: Track success/failure counts to give a clear startup summary.
    int seeded = 0;
    int skipped = 0;
    int failed = 0;

    seeded +=
        seedDomain(
                "system",
                "Handles core system operations, metadata retrieval, configuration management, "
                    + "and framework-level routing logic.")
            ? 1
            : 0;
    seeded +=
        seedDomain(
                "conversational",
                "Manages general chat, greetings, small talk, persona maintenance, "
                    + "and unstructured human-like dialogue.")
            ? 1
            : 0;
    seeded +=
        seedDomain(
                "discovery",
                "Responsible for searching, querying databases, retrieving documents, "
                    + "finding records, and exploring knowledge bases.")
            ? 1
            : 0;
    seeded +=
        seedDomain(
                "execution",
                "Executes commands, triggers workflows, updates records, performs mutations, "
                    + "and interacts with external stateful APIs.")
            ? 1
            : 0;
    seeded +=
        seedDomain(
                "generative",
                "Creates new content, drafts emails, writes code, summarizes text, "
                    + "and synthesizes unstructured creative outputs.")
            ? 1
            : 0;
    seeded +=
        seedDomain(
                "analytical",
                "Performs mathematical calculations, data analysis, statistical aggregation, "
                    + "pattern recognition, and quantitative reasoning.")
            ? 1
            : 0;

    // WHY: Count skipped and failed separately for the summary.
    // Total domains is always 6, so: failed = 6 - seeded - skipped.
    int total = 6;
    skipped = total - seeded - failed;

    log.info(
        "Domain seeding complete. Seeded: {}, Skipped (already exist): {}, Failed: {}",
        seeded,
        skipped,
        failed);
  }

  /**
   * Seeds a single domain if it doesn't already exist.
   *
   * <p>WHY: Returns a boolean (true = seeded, false = skipped or failed) so the caller can track
   * counts. Each call is wrapped in try-catch so one failure doesn't abort the rest.
   *
   * @param name The domain name (unique identifier).
   * @param description The text description to embed as a 384-dim vector.
   * @return true if the domain was newly created and saved, false otherwise.
   */
  private boolean seedDomain(String name, String description) {
    try {
      if (domainRepository.existsByName(name)) {
        log.debug("Domain '{}' already exists. Skipping.", name);
        return false;
      }

      log.info("Seeding domain '{}'...", name);

      // WHY: embed() now returns float[] directly — no List<Double> conversion needed.
      float[] embedding = embeddingService.embed(description);

      Domain domain = new Domain(name, description, embedding);
      domainRepository.save(domain);

      log.info("Successfully seeded domain '{}'.", name);
      return true;
    } catch (Exception e) {
      // WHY: Catch ALL exceptions per domain so one failure doesn't prevent the other 5.
      // This is critical for graceful degradation — if the embedding model is flaky
      // for one input, we still want the other domains available.
      log.error("Failed to seed domain '{}'. Skipping. Error: {}", name, e.getMessage(), e);
      return false;
    }
  }
}
