-- V5__tool_executions.sql

-- Enum for tool status to ensure data integrity
CREATE TYPE tool_status AS ENUM ('PENDING_APPROVAL', 'APPROVED', 'EXECUTED', 'FAILED', 'REJECTED');

-- Tool Executions table for Human-In-The-Loop (HITL) auditing and approval queues
-- WHY: We need to track when an agent attempts a destructive action, pause it,
-- and record the approval or rejection. Even safe actions are recorded here
-- for audit purposes.
-- NOTE: approved_by is currently a placeholder UUID since the users table doesn't exist yet.

CREATE TABLE tool_executions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id        UUID NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    tool_name         VARCHAR(100) NOT NULL,
    input_params      JSONB,
    output            JSONB,
    status            tool_status NOT NULL,
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    approved_by       UUID,
    requested_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at       TIMESTAMPTZ,
    executed_at       TIMESTAMPTZ,
    duration_ms       INTEGER
);

-- WHY: Performance index for the HITL approval queue:
-- "Get all pending HITL approvals" (used in /api/v1/tools/approvals)
CREATE INDEX idx_tool_exec_status ON tool_executions(status);
