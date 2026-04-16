package io.droidevs.mclub.ai.retrieval.vector;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Vector retrieval: embeds query and performs similarity search in pgvector.
 *
 * <p>Currently requires VectorEmbeddingService to be implemented/configured.
 */
@Service
@RequiredArgsConstructor
public class VectorRetrievalService {

    private final VectorEmbeddingService embeddingService;
    private final VectorDbService vectorDbService;

    public List<VectorSearchResult> retrieveSimilar(ConversationContext ctx, String userMessage, int topK) {
        List<Double> emb = embeddingService.embed(userMessage);
        String literal = toPgvectorLiteral(emb);
        return vectorDbService.search(literal, topK, null);

    }

    static String toPgvectorLiteral(List<Double> emb) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < emb.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(emb.get(i));
        }
        sb.append(']');
        return sb.toString();
    }
}

