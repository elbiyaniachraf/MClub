package io.droidevs.mclub.ai.retrieval.vector;

import java.util.UUID;

/** Lightweight DTO for vector search results. */
public record VectorSearchResult(
        UUID id,
        String entityType,
        UUID entityId,
        String content,
        double score
) {
}

