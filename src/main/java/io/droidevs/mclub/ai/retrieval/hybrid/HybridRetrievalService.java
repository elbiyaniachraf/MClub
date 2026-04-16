package io.droidevs.mclub.ai.retrieval.hybrid;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.RetrievalContext;
import io.droidevs.mclub.ai.retrieval.RetrievalService;
import io.droidevs.mclub.ai.retrieval.StructuredRetrievalService;
import io.droidevs.mclub.ai.retrieval.vector.VectorRetrievalService;
import io.droidevs.mclub.ai.retrieval.vector.VectorSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Hybrid retrieval: structured retrieval + (future) vector retrieval.
 *
 * <p>Currently delegates to StructuredRetrievalService only (non-breaking baseline).
 */
@Service
@Primary
@RequiredArgsConstructor
public class HybridRetrievalService implements RetrievalService {

    private final StructuredRetrievalService structured;
    private final ObjectProvider<VectorRetrievalService> vectorRetrievalServiceProvider;

    @Override
    public RetrievalContext retrieve(ConversationContext ctx, String userMessage) {
        RetrievalContext base = structured.retrieve(ctx, userMessage);

        List<String> factual = new ArrayList<>(base.factualSnippets());
        List<String> relevant = new ArrayList<>(base.recentEvents());

        VectorRetrievalService vector = vectorRetrievalServiceProvider.getIfAvailable();
        if (vector == null) {
            factual.add("RetrievalMode: hybrid(structured-only)");
            return new RetrievalContext(factual, relevant);
        }

        try {
            List<VectorSearchResult> hits = vector.retrieveSimilar(ctx, userMessage, 5);
            factual.add("RetrievalMode: hybrid(structured+vector)");
            for (VectorSearchResult hit : hits) {
                relevant.add("[vector score=" + hit.score() + "] " + hit.docType() + " sourceId=" + hit.sourceId() + " :: " + hit.content());
            }
        } catch (Exception e) {
            // Safe fallback (do not break chat) if vector is temporarily unavailable.
            factual.add("RetrievalMode: hybrid(structured-only, vectorError=" + e.getClass().getSimpleName() + ")");
        }

        return new RetrievalContext(factual, relevant);
    }
}


