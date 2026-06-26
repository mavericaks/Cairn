package com.cairn;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import com.cairn.model.OllamaModelManager;

/**
 * Smoke test: Verifies the Spring Boot application context loads successfully. (E1-T7)
 *
 * <p>WHY: This is the most fundamental integration test. If the context fails to load, nothing else
 * works. This test catches:
 *
 * <ul>
 *   <li>Missing beans or circular dependencies
 *   <li>Invalid configuration properties
 *   <li>Flyway migration failures
 *   <li>JPA entity mapping mismatches (ddl-auto=validate)
 *   <li>Auto-configuration conflicts
 * </ul>
 *
 * <p>WHY {@code @Import(TestcontainersConfig.class)}: Pulls in the shared PostgreSQL+pgvector
 * container. Without this, the context would fail because Flyway needs a real PostgreSQL database
 * to run migrations.
 *
 * <p>WHY {@code webEnvironment = RANDOM_PORT}: Starts a real embedded Tomcat on a random port so we
 * can make actual HTTP requests (e.g., health endpoint test). RANDOM_PORT avoids port conflicts
 * when running tests in parallel.
 *
 * @author Cairn Team
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfig.class)
class CairnApplicationTests {

  @Autowired private ApplicationContext context;

  @Autowired private DataSource dataSource;

  @Autowired private TestRestTemplate restTemplate;

  @MockitoBean private OllamaModelManager ollamaModelManager;

  @BeforeEach
  void setUp() {
    when(ollamaModelManager.isOllamaRunning()).thenReturn(true);
    when(ollamaModelManager.isModelAvailable()).thenReturn(true);
  }

  /**
   * WHY: Proves that the entire Spring Boot context loads without errors. This implicitly
   * validates: - All @Configuration classes are valid - All @Bean methods return successfully -
   * Flyway migrations complete - JPA entity validation passes - No auto-configuration conflicts
   */
  @Test
  @DisplayName("Application context loads successfully")
  void contextLoads() {
    assertThat(context).isNotNull();
  }

  /**
   * WHY: Verifies the application name matches what we configured. This is a sanity check that
   * application.yml is being read correctly.
   */
  @Test
  @DisplayName("Application name is 'cairn'")
  void applicationNameIsConfigured() {
    String appName = context.getEnvironment().getProperty("spring.application.name");
    assertThat(appName).isEqualTo("cairn");
  }

  /**
   * WHY: Verifies Flyway migrations ran successfully and the 'domains' table exists. This proves
   * V1__base_schema_with_pgvector.sql was applied correctly. Also validates that the pgvector
   * extension is installed (required by the migration).
   */
  @Test
  @DisplayName("Flyway V1 migration created 'domains' table with pgvector")
  void flywayMigrationApplied() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      // WHY: Query the information_schema to verify the 'domains' table exists.
      // This is database-agnostic (works with any JDBC-compliant DB).
      ResultSet tables = conn.getMetaData().getTables(null, "public", "domains", null);
      assertThat(tables.next())
          .as("'domains' table should exist after Flyway V1 migration")
          .isTrue();

      // WHY: Verify pgvector extension is installed by checking pg_extension.
      // If this fails, the pgvector Docker image is not being used correctly.
      ResultSet extensions =
          conn.createStatement()
              .executeQuery("SELECT extname FROM pg_extension WHERE extname = 'vector'");
      assertThat(extensions.next()).as("pgvector extension should be installed").isTrue();
    }
  }

  /**
   * WHY: Verifies virtual threads are enabled via configuration. Virtual threads (Java 21 / Project
   * Loom) are critical for Cairn's agent orchestration concurrency model (Epic 3).
   */
  @Test
  @DisplayName("Virtual threads are enabled")
  void virtualThreadsEnabled() {
    String virtualEnabled = context.getEnvironment().getProperty("spring.threads.virtual.enabled");
    assertThat(virtualEnabled).isEqualTo("true");
  }

  /**
   * WHY: Verifies the Actuator health endpoint responds with HTTP 200 and a JSON body containing
   * "status":"UP". This is the exact endpoint Railway will probe for health checks (E1-T9,
   * ADR-005).
   *
   * <p>This test catches:
   *
   * <ul>
   *   <li>Actuator not auto-configured (missing starter-actuator)
   *   <li>Health endpoint not exposed (misconfigured management.endpoints)
   *   <li>Database health indicator failing (datasource issues)
   * </ul>
   */
  @Test
  @DisplayName("Actuator health endpoint returns UP (Railway health check)")
  void actuatorHealthEndpointIsUp() {
    ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

    assertThat(response.getStatusCode())
        .as("Health endpoint should return 200 OK")
        .isEqualTo(HttpStatus.OK);

    assertThat(response.getBody())
        .as("Health response should contain UP status")
        .contains("\"status\":\"UP\"");
  }
}
