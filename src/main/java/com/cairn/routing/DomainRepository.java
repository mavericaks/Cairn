package com.cairn.routing;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for interacting with semantic Domains.
 *
 * <p>WHY: Provides standard CRUD operations to allow the DomainSeeder to check existence and insert
 * the foundational domains.
 */
@Repository
public interface DomainRepository extends JpaRepository<Domain, UUID> {

  /**
   * WHY: Checks if a domain exists to make seeding idempotent.
   *
   * @param name The domain name.
   * @return True if the domain already exists in the database.
   */
  boolean existsByName(String name);

  /**
   * WHY: Fetch a domain by its unique name to append examples if they are missing.
   *
   * @param name The domain name.
   * @return The Domain if found.
   */
  Optional<Domain> findByName(String name);
}
