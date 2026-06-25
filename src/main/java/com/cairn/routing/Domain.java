package com.cairn.routing;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * Represents a semantic routing domain in the database.
 *
 * <p>WHY: This entity maps to the domains table which stores the pre-computed embeddings of our 6
 * foundational domains. The semantic router uses these vectors to perform HNSW cosine similarity
 * searches to classify user intent.
 */
@Entity
@Table(name = "domains")
public class Domain {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 100)
  private String name;

  @Column(columnDefinition = "TEXT")
  private String description;

  // WHY: Domains can be soft-deleted (set active=false) without losing data.
  // The DomainRouter only searches active domains.
  @Column(nullable = false)
  private boolean active = true;

  // WHY: Maps the PostgreSQL vector(384) column natively to a float[] using hibernate-vector.
  @Column(nullable = false, columnDefinition = "vector(384)")
  @JdbcTypeCode(SqlTypes.VECTOR)
  private float[] embedding;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  // WHY: Bidirectional relationship mapped by the 'domain' field in DomainExample.
  // CascadeType.ALL ensures examples are saved/deleted with the domain.
  // orphanRemoval=true deletes examples if they are removed from the list.
  @OneToMany(mappedBy = "domain", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<DomainExample> examples = new ArrayList<>();

  // Constructors
  public Domain() {}

  public Domain(String name, String description, float[] embedding) {
    this.name = name;
    this.description = description;
    this.embedding = embedding;
  }

  // Getters
  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public boolean isActive() {
    return active;
  }

  /**
   * Returns a defensive copy of the embedding array.
   *
   * <p>WHY: Prevents callers from accidentally mutating the internal state of this entity.
   * Defensive copies are a standard Java encapsulation practice.
   */
  public float[] getEmbedding() {
    return embedding != null ? Arrays.copyOf(embedding, embedding.length) : null;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public List<DomainExample> getExamples() {
    return examples;
  }

  // Setters
  public void setId(UUID id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public void setEmbedding(float[] embedding) {
    this.embedding = embedding;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public void setExamples(List<DomainExample> examples) {
    this.examples = examples;
  }

  // Helper methods
  public void addExample(DomainExample example) {
    examples.add(example);
    example.setDomain(this);
  }

  public void removeExample(DomainExample example) {
    examples.remove(example);
    example.setDomain(null);
  }
}
