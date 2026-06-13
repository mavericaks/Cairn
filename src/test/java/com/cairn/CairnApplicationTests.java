package com.cairn;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test: Verifies the Spring Boot application context loads successfully. (E1-T7)
 *
 * <p>WHY: This is the most fundamental integration test. If the context fails to load,
 * nothing else works. This test catches:</p>
 * <ul>
 *     <li>Missing beans or circular dependencies</li>
 *     <li>Invalid configuration properties</li>
 *     <li>Flyway migration failures</li>
 *     <li>JPA entity mapping mismatches (ddl-auto=validate)</li>
 *     <li>Auto-configuration conflicts</li>
 * </ul>
 *
 * <p>WHY {@code @Import(TestcontainersConfig.class)}: Pulls in the shared
 * PostgreSQL+pgvector container. Without this, the context would fail because
 * Flyway needs a real PostgreSQL database to run migrations.</p>
 *
 * @author Cairn Team
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
class CairnApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private DataSource dataSource;

    /**
     * WHY: Proves that the entire Spring Boot context loads without errors.
     * This implicitly validates:
     * - All @Configuration classes are valid
     * - All @Bean methods return successfully
     * - Flyway migrations complete
     * - JPA entity validation passes
     * - No auto-configuration conflicts
     */
    @Test
    @DisplayName("Application context loads successfully")
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    /**
     * WHY: Verifies the application name matches what we configured.
     * This is a sanity check that application.yml is being read correctly.
     */
    @Test
    @DisplayName("Application name is 'cairn'")
    void applicationNameIsConfigured() {
        String appName = context.getEnvironment().getProperty("spring.application.name");
        assertThat(appName).isEqualTo("cairn");
    }

    /**
     * WHY: Verifies Flyway migrations ran successfully and the 'domains' table exists.
     * This proves V1__base_schema_with_pgvector.sql was applied correctly.
     * Also validates that the pgvector extension is installed (required by the migration).
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
            ResultSet extensions = conn.createStatement()
                    .executeQuery("SELECT extname FROM pg_extension WHERE extname = 'vector'");
            assertThat(extensions.next())
                    .as("pgvector extension should be installed")
                    .isTrue();
        }
    }

    /**
     * WHY: Verifies virtual threads are enabled via configuration.
     * Virtual threads (Java 21 / Project Loom) are critical for Cairn's
     * agent orchestration concurrency model (Epic 3).
     */
    @Test
    @DisplayName("Virtual threads are enabled")
    void virtualThreadsEnabled() {
        String virtualEnabled = context.getEnvironment()
                .getProperty("spring.threads.virtual.enabled");
        assertThat(virtualEnabled).isEqualTo("true");
    }
}
