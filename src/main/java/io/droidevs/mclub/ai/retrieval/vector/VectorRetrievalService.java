package io.droidevs.mclub.ai.retrieval.vector;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Vector retrieval: embeds query and performs similarity search in pgvector.
 */
@Service
@RequiredArgsConstructor
public class VectorRetrievalService {

    private final EmbeddingService embeddingService;
    private final VectorIndexRepository vectorIndexRepository;

    public List<VectorSearchResult> retrieveSimilar(ConversationContext ctx, String userMessage, int topK) {
        List<Double> emb = embeddingService.embed(userMessage);
        String literal = VectorSearchService.toPgvectorLiteral(emb);

        java.util.UUID clubId = ctx != null ? ctx.clubScopeId().orElse(null) : null;
        java.util.UUID eventId = ctx != null ? ctx.eventScopeId().orElse(null) : null;

        // If eventId is present, it’s the strongest scope. Club scope can remain as an additional filter.
        return vectorIndexRepository.search(literal, topK, null, clubId, eventId);
    }

    // Keep helper for any other callers
    static String toPgvectorLiteral(List<Double> emb) {
        return VectorSearchService.toPgvectorLiteral(emb);
    }
}
