package com.cairn;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared Testcontainers configuration for all integration tests. (ADR-008)
 *
 * <p>WHY: Provides a single, reusable PostgreSQL+pgvector container that is shared
 * across all {@code @SpringBootTest} classes in the test suite. This avoids starting
 * a new container for every test class (which would be slow) while ensuring each
 * test run gets a fresh, disposable database.</p>
 *
 * <p>WHY {@code @ServiceConnection}: Spring Boot 3.1+ auto-configures
 * {@code spring.datasource.url}, {@code username}, and {@code password} from the
 * container — zero manual property plumbing. This replaces the old
 * {@code @DynamicPropertySource} approach.</p>
 *
 * <p>WHY {@code pgvector/pgvector:pg17}: Our V1 Flyway migration runs
 * {@code CREATE EXTENSION IF NOT EXISTS vector}, which requires pgvector to be
 * installed in the PostgreSQL image. Plain {@code postgres:17} would fail.</p>
 *
 * <p>Usage: Import this configuration in any {@code @SpringBootTest}:
 * <pre>{@code
 * @SpringBootTest
 * @Import(TestcontainersConfig.class)
 * class MyTest { ... }
 * }</pre></p>
 *
 * @author Cairn Team
 * @see org.springframework.boot.testcontainers.service.connection.ServiceConnection
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    /**
     * Creates a PostgreSQL container with pgvector extension for integration tests.
     *
     * <p>WHY: Static container with {@code @Bean} + {@code @ServiceConnection} means
     * Spring Boot manages the container lifecycle tied to the application context.
     * The container starts once per context (shared across tests that use the same
     * context via context caching) and stops when the context is destroyed.</p>
     *
     * <p>WHY {@code withReuse(true)}: Enables Testcontainers' reuse feature — if the
     * developer has {@code testcontainers.reuse.enable=true} in their
     * {@code ~/.testcontainers.properties}, the container persists between test runs.
     * Cuts subsequent test runs from ~10s container startup to ~0s.</p>
     *
     * @return a configured PostgreSQLContainer ready for Spring Boot auto-configuration
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgreSQLContainer() {
        return new PostgreSQLContainer<>("pgvector/pgvector:pg17")
                .withDatabaseName("cairn_test")
                .withUsername("cairn")
                .withPassword("cairn_test")
                .withReuse(true);
    }
}
