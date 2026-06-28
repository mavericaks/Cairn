package com.cairn.routing;

import com.cairn.model.LocalEmbeddingService;
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
                    + "and framework-level routing logic.",
                List.of(
                    "Why is the app running so slow?",
                    "Check the memory usage of the server.",
                    "Read the error logs from last night.",
                    "Are there any deadlocks in the database?",
                    "Show me the CPU utilization for the past hour.",
                    "Diagnose the OutOfMemory exception.",
                    "What is the current status of the Kafka cluster?",
                    "Is the Redis cache hitting its memory limit?",
                    "Explain this stack trace.",
                    "Monitor the network traffic for anomalies."))
            ? 1
            : 0;
    seeded +=
        seedDomain(
                "conversational",
                "Manages general chat, greetings, small talk, persona maintenance, "
                    + "and unstructured human-like dialogue.",
                List.of(
                    "Hello, how are you today?",
                    "That's a very helpful answer, thank you.",
                    "I'm feeling frustrated with my code.",
                    "What's the meaning of life?",
                    "Can you tell me a joke?",
                    "I don't understand what you mean.",
                    "Who created you?",
                    "Good morning!",
                    "That makes sense now, thanks.",
                    "You are acting weird today."))
            ? 1
            : 0;
    seeded +=
        seedDomain(
                "discovery",
                "Responsible for searching, querying databases, retrieving documents, "
                    + "finding records, and exploring knowledge bases.",
                List.of(
                    "What is the company policy on remote work?",
                    "Find the documentation for the API.",
                    "Search my files for the Q3 financial report.",
                    "Where can I find the onboarding guide?",
                    "Can you pull up the meeting notes from last Tuesday?",
                    "Look for any documents related to project Alpha.",
                    "Retrieve the architecture diagrams.",
                    "Query the knowledge base for troubleshooting steps.",
                    "Find the latest release notes.",
                    "Search for the design specifications."))
            ? 1
            : 0;
    seeded +=
        seedDomain(
                "execution",
                "Executes commands, triggers workflows, updates records, performs mutations, "
                    + "and interacts with external stateful APIs.",
                List.of(
                    "Restart the production server.",
                    "Send an email to the marketing team.",
                    "Deploy the latest code to staging.",
                    "Delete the temporary files in the cache.",
                    "Update the user's role to admin.",
                    "Create a new Jira ticket for the bug.",
                    "Trigger the nightly backup job.",
                    "Reset the password for user john_doe.",
                    "Cancel the pending order.",
                    "Provision a new database instance."))
            ? 1
            : 0;
    seeded +=
        seedDomain(
                "generative",
                "Creates new content, drafts emails, writes code, summarizes text, "
                    + "and synthesizes unstructured creative outputs.",
                List.of(
                    "Create a Spring Boot controller for user management.",
                    "Generate an image of a futuristic city.",
                    "Write a boilerplate React component.",
                    "Draft a welcome email for new employees.",
                    "Write a blog post about artificial intelligence.",
                    "Summarize this long article into 3 bullet points.",
                    "Create a poem about a robot learning to love.",
                    "Generate a SQL schema for an e-commerce store.",
                    "Write a python script to scrape a website.",
                    "Translate this paragraph into French."))
            ? 1
            : 0;
    seeded +=
        seedDomain(
                "analytical",
                "Performs mathematical calculations, data analysis, statistical aggregation, "
                    + "pattern recognition, and quantitative reasoning.",
                List.of(
                    "How many users signed up last month?",
                    "Give me a breakdown of revenue by region.",
                    "What is the average latency of our API?",
                    "Calculate the week-over-week growth rate.",
                    "Summarize the sales figures for Q4.",
                    "What percentage of our traffic comes from mobile?",
                    "Find the top 5 most active customers.",
                    "Compare the performance metrics between server A and B.",
                    "Show me the distribution of error codes.",
                    "Generate a statistical analysis of the user retention data."))
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
   * @param examples A list of 10+ few-shot routing examples to embed.
   * @return true if the domain was newly created and saved, false otherwise.
   */
  private boolean seedDomain(String name, String description, List<String> examples) {
    try {
      Domain domain = domainRepository.findByName(name).orElse(null);
      boolean isNew = false;

      if (domain == null) {
        log.info("Seeding domain '{}'...", name);
        // WHY: embed() now returns float[] directly — no List<Double> conversion needed.
        float[] embedding = embeddingService.embed(description);
        domain = new Domain(name, description, embedding);
        isNew = true;
      } else {
        if (!domain.getExamples().isEmpty()) {
          log.debug("Domain '{}' already has examples. Skipping.", name);
          return false;
        }
        log.info(
            "Domain '{}' exists but missing examples. Seeding {} examples...",
            name,
            examples.size());
      }

      for (String exampleText : examples) {
        float[] exampleEmbedding = embeddingService.embed(exampleText);
        domain.addExample(new DomainExample(domain, exampleText, exampleEmbedding));
      }

      domainRepository.save(domain);

      log.info("Successfully seeded domain '{}'.", name);
      return isNew;
    } catch (Exception e) {
      // WHY: Catch ALL exceptions per domain so one failure doesn't prevent the other 5.
      // This is critical for graceful degradation — if the embedding model is flaky
      // for one input, we still want the other domains available.
      log.error("Failed to seed domain '{}'. Skipping. Error: {}", name, e.getMessage(), e);
      return false;
    }
  }
}
