package com.cairn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Cairn AI orchestration platform.
 *
 * <p>WHY: This class's package ({@code com.cairn}) serves as the root for Spring Modulith module
 * scanning. Each sub-package (routing, agents, tools, observability, security) becomes an isolated
 * module with enforced boundaries — no microservice overhead, but compile-time architectural
 * enforcement (ADR-001).
 *
 * <p>WHY: {@code @SpringBootApplication} is placed here (and only here) to ensure a single
 * component scan root. Spring Modulith relies on this convention to discover and enforce module
 * boundaries.
 *
 * @author Cairn Team
 * @see <a href="https://docs.spring.io/spring-modulith/reference/">Spring Modulith Reference</a>
 */
@SpringBootApplication
public class CairnApplication {

  private static final Logger log = LoggerFactory.getLogger(CairnApplication.class);

  /**
   * Bootstraps the Cairn platform with embedded Tomcat and Spring context.
   *
   * <p>WHY: Uses standard Spring Boot bootstrapping rather than custom builders. Virtual threads
   * are enabled via {@code spring.threads.virtual.enabled=true} in configuration, not
   * programmatically — keeps the entry point clean and configuration externalized.
   *
   * @param args command-line arguments forwarded to Spring Boot
   */
  public static void main(String[] args) {
    log.info("Starting Cairn — extensible AI orchestration platform");
    SpringApplication.run(CairnApplication.class, args);
    log.info("Cairn started successfully");
  }
}
