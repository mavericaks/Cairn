-- V7__analytics_and_audit_log.sql
-- WHY: Creates the event-driven analytics infrastructure. Every action in Cairn
-- (chat, routing, tool calls, document uploads) is published to Kafka and consumed
-- into these tables for real-time dashboards and immutable audit trails.

-- Immutable audit log — every platform event is persisted as a JSONB row.
-- This table is append-only and should never be updated or deleted.
CREATE TABLE IF NOT EXISTS audit_log (
    id              BIGSERIAL PRIMARY KEY,
    event_type      VARCHAR(50)     NOT NULL,
    event_source    VARCHAR(100)    NOT NULL,
    user_id         UUID,
    payload         JSONB           NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Index on event_type + created_at for filtered time-range queries
CREATE INDEX IF NOT EXISTS idx_audit_log_type_time ON audit_log (event_type, created_at DESC);
-- Index on user_id for per-user audit queries
CREATE INDEX IF NOT EXISTS idx_audit_log_user ON audit_log (user_id) WHERE user_id IS NOT NULL;

-- Routing analytics — aggregated routing decisions per domain per hour
CREATE TABLE IF NOT EXISTS analytics_routing (
    id              BIGSERIAL PRIMARY KEY,
    domain_name     VARCHAR(50)     NOT NULL,
    hour_bucket     TIMESTAMPTZ     NOT NULL,
    hit_count       INTEGER         NOT NULL DEFAULT 0,
    avg_score       DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    avg_latency_ms  DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    UNIQUE (domain_name, hour_bucket)
);

-- Usage analytics — per-user per-day message counts
CREATE TABLE IF NOT EXISTS analytics_usage (
    id              BIGSERIAL PRIMARY KEY,
    user_id         UUID            NOT NULL,
    day_bucket      DATE            NOT NULL,
    message_count   INTEGER         NOT NULL DEFAULT 0,
    token_count     BIGINT          NOT NULL DEFAULT 0,
    UNIQUE (user_id, day_bucket)
);

-- Tool analytics — per-tool execution stats
CREATE TABLE IF NOT EXISTS analytics_tools (
    id              BIGSERIAL PRIMARY KEY,
    tool_name       VARCHAR(100)    NOT NULL,
    day_bucket      DATE            NOT NULL,
    call_count      INTEGER         NOT NULL DEFAULT 0,
    approval_count  INTEGER         NOT NULL DEFAULT 0,
    rejection_count INTEGER         NOT NULL DEFAULT 0,
    avg_duration_ms DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    UNIQUE (tool_name, day_bucket)
);
