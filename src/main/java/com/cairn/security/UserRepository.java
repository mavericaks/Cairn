package com.cairn.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * WHY: Repository for User entities.
 */
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByGithubId(String githubId);
}
