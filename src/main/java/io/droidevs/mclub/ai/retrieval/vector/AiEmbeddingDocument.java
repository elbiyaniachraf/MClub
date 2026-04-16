package io.droidevs.mclub.ai.retrieval.vector;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Entity mapped to ai_embedding_document table (pgvector). */
@Getter
@Setter
@Entity
@Table(name = "ai_embedding_document")
public class AiEmbeddingDocument {

    @Id
    private UUID id;

    @Column(name = "entity_type", nullable = false, length = 16)
    private String entityType;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "club_id")
    private UUID clubId;

    @Column(name = "activity_id")
    private UUID activityId;

    @Column(name = "comment_id")
    private UUID commentId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "metadata", nullable = false, columnDefinition = "jsonb")
    private String metadataJson;

    /**
     * Vector embedding stored as text representation "[0.1,0.2,...]".
     *
     * <p>We intentionally avoid adding a pgvector-specific Java type to keep the dependency surface small.
     * We use native SQL for similarity queries and writes.
     */
    @Column(name = "embedding", nullable = false, columnDefinition = "vector(1536)")
    private String embedding;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}

