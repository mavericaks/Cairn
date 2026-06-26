package com.cairn.agents;

import com.cairn.routing.Domain;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

/**
 * Represents a single turn in a conversation.
 *
 * <p>WHY: Maps to the 'messages' table. Stores not just the text content, but the routing metadata
 * (domain, score, latency) and LLM metadata (token count) for analytics and debugging.
 */
@Entity
@Table(name = "messages")
public class Message {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "conversation_id", nullable = false)
  private Conversation conversation;

  // WHY: We use @JdbcType(PostgreSQLEnumJdbcType.class) to map the Java Enum directly to
  // the custom Postgres ENUM type we defined in Flyway. This avoids storing strings.
  @Enumerated(EnumType.STRING)
  @JdbcType(PostgreSQLEnumJdbcType.class)
  @Column(name = "role", nullable = false)
  private MessageRole role;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "routed_domain_id")
  private Domain routedDomain;

  @Column(name = "routing_score")
  private Float routingScore;

  @Column(name = "token_count")
  private Integer tokenCount;

  @Column(name = "duration_ms")
  private Integer durationMs;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected Message() {
    // JPA requires a no-arg constructor
  }

  public Message(MessageRole role, String content) {
    this.role = role;
    this.content = content;
  }

  @PrePersist
  protected void onCreate() {
    if (this.createdAt == null) {
      this.createdAt = Instant.now();
    }
  }

  // Getters and Setters

  public UUID getId() {
    return id;
  }

  public Conversation getConversation() {
    return conversation;
  }

  public void setConversation(Conversation conversation) {
    this.conversation = conversation;
  }

  public MessageRole getRole() {
    return role;
  }

  public String getContent() {
    return content;
  }

  public Domain getRoutedDomain() {
    return routedDomain;
  }

  public void setRoutedDomain(Domain routedDomain) {
    this.routedDomain = routedDomain;
  }

  public Float getRoutingScore() {
    return routingScore;
  }

  public void setRoutingScore(Float routingScore) {
    this.routingScore = routingScore;
  }

  public Integer getTokenCount() {
    return tokenCount;
  }

  public void setTokenCount(Integer tokenCount) {
    this.tokenCount = tokenCount;
  }

  public Integer getDurationMs() {
    return durationMs;
  }

  public void setDurationMs(Integer durationMs) {
    this.durationMs = durationMs;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
