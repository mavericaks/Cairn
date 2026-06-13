-- ═══════════════════════════════════════════════════════════════════
-- V1: Base Schema — pgvector extension + domains table
-- ═══════════════════════════════════════════════════════════════════
-- WHY: This is the foundational migration for Cairn. It enables the
-- pgvector extension for vector similarity search and creates the
-- domains table used by the routing module (Epic 2) to classify
-- user intent via semantic embedding comparison.
--
-- Flyway naming: V1__base_schema_with_pgvector.sql
--   V1  = version 1 (first migration, applied in order)
--   __  = Flyway separator (double underscore)
--   base_schema_with_pgvector = human-readable description
-- ═══════════════════════════════════════════════════════════════════

-- WHY: pgvector provides the 'vector' data type and similarity operators
-- (cosine, L2, inner product). Required for semantic routing (ADR-002).
-- IF NOT EXISTS ensures idempotency if the extension was pre-created.
CREATE EXTENSION IF NOT EXISTS vector;

-- ═══════════════════════════════════════════════════════════════════
-- Domains table — the routing knowledge base
-- ═══════════════════════════════════════════════════════════════════
-- WHY: Each row represents a domain that Cairn can route to.
-- When a user sends a message, routing embeds it (MiniLM, 384-dim)
-- and finds the nearest domain embedding via HNSW index.
--
-- Example domains: "database-performance", "code-review", "devops"
-- Each domain has a pre-computed embedding of its description.
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE domains (
    -- WHY: UUID primary keys prevent sequential ID enumeration in APIs.
    -- gen_random_uuid() is built into PostgreSQL 13+ (no pgcrypto needed).
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- WHY: Unique name is the human-readable identifier for the domain.
    -- Used in logs, observability events, and admin UI.
    name            VARCHAR(100) NOT NULL UNIQUE,

    -- WHY: Description is the text that gets embedded into the vector.
    -- A good description = better routing accuracy.
    description     TEXT,

    -- WHY: 384 dimensions matches MiniLM-L6-v2 output (ADR-002).
    -- NOT NULL because a domain without an embedding cannot be routed to.
    embedding       vector(384) NOT NULL,

    -- WHY: Timestamps for auditability. TIMESTAMPTZ stores UTC internally,
    -- renders in the client's timezone. Always use TIMESTAMPTZ, never TIMESTAMP.
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ═══════════════════════════════════════════════════════════════════
-- HNSW index for fast approximate nearest neighbor search
-- ═══════════════════════════════════════════════════════════════════
-- WHY: HNSW (Hierarchical Navigable Small World) provides sub-linear
-- search time for nearest neighbors. Proven ~20ms routing latency
-- in previous build (ADR-002).
--
-- vector_cosine_ops: uses cosine similarity, which is invariant to
-- vector magnitude — correct for comparing semantic embeddings
-- where direction matters more than length.
-- ═══════════════════════════════════════════════════════════════════
CREATE INDEX idx_domains_embedding
    ON domains USING hnsw (embedding vector_cosine_ops);
