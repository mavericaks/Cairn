-- V4__conversations_messages.sql

-- WHY: We need a schema for persisting chat history. We use gen_random_uuid() for PKs.
-- user_id is currently a placeholder UUID since the users table doesn't exist yet (Epic 5).
-- No FK constraint on user_id yet.

CREATE TABLE conversations (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL,
    title          VARCHAR(255),
    last_domain_id UUID REFERENCES domains(id) ON DELETE SET NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Enum for message roles to ensure data integrity
CREATE TYPE message_role AS ENUM ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL');

-- Messages table
CREATE TABLE messages (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id  UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role             message_role NOT NULL,
    content          TEXT NOT NULL,
    routed_domain_id UUID REFERENCES domains(id) ON DELETE SET NULL,
    routing_score    REAL,
    token_count      INTEGER,
    duration_ms      INTEGER,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- WHY: Performance indexes for the most common query patterns:
-- 1. "Get all conversations for user X" (used in /api/v1/conversations)
-- 2. "Get all messages for conversation Y" (used in /api/v1/conversations/{id}/messages)
-- 3. "Get last N messages ordered by time" (used when building LLM context)
CREATE INDEX idx_conversations_user_id ON conversations(user_id);
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);
