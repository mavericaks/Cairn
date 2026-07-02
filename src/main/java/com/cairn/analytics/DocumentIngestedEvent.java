package com.cairn.analytics;

import java.time.Instant;
import java.util.UUID;

/**
 * WHY: Published to Kafka when a document is uploaded and ingested into the RAG pipeline. Tracks
 * ingestion volume and document processing metadata.
 */
public record DocumentIngestedEvent(
    UUID userId, String filename, int chunkCount, long processingTimeMs, Instant timestamp) {

  public static DocumentIngestedEvent of(
      UUID userId, String filename, int chunkCount, long processingTimeMs) {
    return new DocumentIngestedEvent(userId, filename, chunkCount, processingTimeMs, Instant.now());
  }
}
