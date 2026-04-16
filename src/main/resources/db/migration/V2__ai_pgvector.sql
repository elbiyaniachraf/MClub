-- Enable pgvector extension (must be installed on the Postgres image)
CREATE EXTENSION IF NOT EXISTS vector;

-- Semantic index layer for RAG (NOT a source of truth)
-- References existing entities via nullable FK columns.
CREATE TABLE IF NOT EXISTS ai_embedding_document (
    id UUID PRIMARY KEY,

    entity_type VARCHAR(16) NOT NULL CHECK (entity_type IN ('EVENT','CLUB','ACTIVITY','COMMENT')),

    -- Schema-aware entity references (exactly one should be non-null)
    event_id UUID NULL,
    club_id UUID NULL,
    activity_id UUID NULL,
    comment_id UUID NULL,

    chunk_index INT NOT NULL DEFAULT 0,
    content TEXT NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,

    embedding vector(1536) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_ai_embedding_one_ref CHECK (
        (CASE WHEN event_id IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN club_id IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN activity_id IS NULL THEN 0 ELSE 1 END) +
        (CASE WHEN comment_id IS NULL THEN 0 ELSE 1 END)
        = 1
    ),

    CONSTRAINT fk_ai_emb_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_emb_club FOREIGN KEY (club_id) REFERENCES clubs(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_emb_activity FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_emb_comment FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ai_embedding_document_entity_type_idx ON ai_embedding_document(entity_type);
CREATE INDEX IF NOT EXISTS ai_embedding_document_event_id_idx ON ai_embedding_document(event_id);
CREATE INDEX IF NOT EXISTS ai_embedding_document_club_id_idx ON ai_embedding_document(club_id);
CREATE INDEX IF NOT EXISTS ai_embedding_document_activity_id_idx ON ai_embedding_document(activity_id);
CREATE INDEX IF NOT EXISTS ai_embedding_document_comment_id_idx ON ai_embedding_document(comment_id);

-- Approx nearest neighbor search index
CREATE INDEX IF NOT EXISTS ai_embedding_document_embedding_hnsw
    ON ai_embedding_document USING hnsw (embedding vector_cosine_ops);

