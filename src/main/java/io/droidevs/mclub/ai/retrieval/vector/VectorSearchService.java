package io.droidevs.mclub.ai.retrieval.vector;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/** Service layer facade for vector search. */
@Service
@RequiredArgsConstructor
public class VectorSearchService {

    private final EmbeddingService embeddingService;
    private final VectorIndexRepository repo;

    public List<VectorSearchResult> semanticSearch(ConversationContext ctx,
                                                  String query,
                                                  int topK,
                                                  String entityType,
                                                  UUID clubId,
                                                  UUID eventId) {
        List<Double> emb = embeddingService.embed(query);
        return repo.search(toPgvectorLiteral(emb), topK, entityType, clubId, eventId);
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

