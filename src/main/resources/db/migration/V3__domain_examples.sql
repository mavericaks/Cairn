-- V3: Create domain_examples table for Few-Shot Vector Routing
-- WHY: Routing against 6 domain descriptions is not accurate enough.
-- We seed 10+ real-world example prompts per domain. When a user asks a question,
-- we find the closest matching example vector via HNSW and route to its parent domain.

CREATE TABLE domain_examples (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    domain_id UUID NOT NULL REFERENCES domains(id) ON DELETE CASCADE,
    example_text TEXT NOT NULL,
    embedding vector(384) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- WHY: HNSW (Hierarchical Navigable Small World) provides massive speed and recall
-- improvements over IVFFlat, and does not require the table to be pre-populated
-- before index creation. We use vector_cosine_ops for cosine similarity.
CREATE INDEX idx_domain_examples_embedding ON domain_examples USING hnsw (embedding vector_cosine_ops);
