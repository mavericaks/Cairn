-- V6__users_and_security.sql

-- Enum for User roles
CREATE TYPE user_role AS ENUM ('USER', 'ADMIN');

-- Users table
-- WHY: Stores user identities from OAuth2 (GitHub) and assigns roles.
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    github_id     VARCHAR(255) UNIQUE NOT NULL,
    email         VARCHAR(255),
    username      VARCHAR(255) NOT NULL,
    role          user_role NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_github_id ON users(github_id);
CREATE INDEX idx_users_email ON users(email);

-- Link conversations to users
-- Note: Up until now, conversations.user_id was a UUID without an FK.
-- In a real production migration with existing data, we would either 
-- map existing UUIDs to users or wipe the table. Since this is local dev,
-- we'll just add the foreign key.
ALTER TABLE conversations
    ADD CONSTRAINT fk_conversations_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Link tool_executions to users
-- Note: tool_executions.approved_by was a UUID without an FK.
ALTER TABLE tool_executions
    ADD CONSTRAINT fk_tool_executions_user
    FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL;
