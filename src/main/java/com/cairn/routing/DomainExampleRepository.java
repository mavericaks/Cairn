package com.cairn.routing;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing DomainExample persistence.
 *
 * <p>WHY: Provides basic CRUD. Note that the actual pgvector cosine similarity search is too
 * complex for Spring Data JPA derived queries and will be handled via a native query in the
 * DomainRouter (E2-T6).
 */
@Repository
public interface DomainExampleRepository extends JpaRepository<DomainExample, UUID> {
  // Empty for now — native query for pgvector will live in DomainRouter
}
