package com.cairn.routing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Represents a single example query mapped to a foundational domain.
 *
 * <p>WHY: By storing 10+ specific examples per domain (Few-Shot routing), we give the vector math a
 * much wider surface area to match against user intent, dramatically increasing routing accuracy
 * compared to just embedding a single domain description.
 */
@Entity
@Table(name = "domain_examples")
public class DomainExample {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  // WHY: Lazy fetching prevents N+1 queries if we ever pull large lists of examples.
  // The router relies on native SQL, but JPA still needs the mapping.
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "domain_id", nullable = false)
  private Domain domain;

  @Column(name = "example_text", nullable = false, columnDefinition = "TEXT")
  private String exampleText;

  // WHY: Maps the PostgreSQL vector(384) column natively to a float[].
  @Column(nullable = false, columnDefinition = "vector(384)")
  @JdbcTypeCode(SqlTypes.VECTOR)
  private float[] embedding;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  // Constructors
  public DomainExample() {}

  public DomainExample(Domain domain, String exampleText, float[] embedding) {
    this.domain = domain;
    this.exampleText = exampleText;
    this.embedding = embedding;
  }

  // Getters
  public UUID getId() {
    return id;
  }

  public Domain getDomain() {
    return domain;
  }

  public String getExampleText() {
    return exampleText;
  }

  /**
   * Returns a defensive copy of the embedding array.
   *
   * <p>WHY: Prevents callers from accidentally mutating the internal state.
   */
  public float[] getEmbedding() {
    return embedding != null ? Arrays.copyOf(embedding, embedding.length) : null;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  // Setters
  public void setId(UUID id) {
    this.id = id;
  }

  public void setDomain(Domain domain) {
    this.domain = domain;
  }

  public void setExampleText(String exampleText) {
    this.exampleText = exampleText;
  }

  public void setEmbedding(float[] embedding) {
    this.embedding = embedding;
  }
}
