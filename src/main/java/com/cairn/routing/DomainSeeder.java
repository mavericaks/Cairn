package com.cairn.routing;

import java.util.List;
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

    seedDomain(
        "system",
        "Handles core system operations, metadata retrieval, configuration management, and framework-level routing logic.");
    seedDomain(
        "conversational",
        "Manages general chat, greetings, small talk, persona maintenance, and unstructured human-like dialogue.");
    seedDomain(
        "discovery",
        "Responsible for searching, querying databases, retrieving documents, finding records, and exploring knowledge bases.");
    seedDomain(
        "execution",
        "Executes commands, triggers workflows, updates records, performs mutations, and interacts with external stateful APIs.");
    seedDomain(
        "generative",
        "Creates new content, drafts emails, writes code, summarizes text, and synthesizes unstructured creative outputs.");
    seedDomain(
        "analytical",
        "Performs mathematical calculations, data analysis, statistical aggregation, pattern recognition, and quantitative reasoning.");

    log.info("Domain seeding complete.");
  }

  private void seedDomain(String name, String description) {
    if (domainRepository.existsByName(name)) {
      log.debug("Domain '{}' already exists. Skipping.", name);
      return;
    }

    log.info("Seeding domain '{}'...", name);

    // Calculate the embedding dynamically
    List<Double> embeddingList = embeddingService.embed(description);

    // Convert List<Double> to float[] for hibernate-vector
    float[] embeddingArray = new float[embeddingList.size()];
    for (int i = 0; i < embeddingList.size(); i++) {
      embeddingArray[i] = embeddingList.get(i).floatValue();
    }

    Domain domain = new Domain(name, description, embeddingArray);
    domainRepository.save(domain);

    log.info("Successfully seeded domain '{}'.", name);
  }
}
