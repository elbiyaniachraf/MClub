package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.ai.retrieval.vector.VectorSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Tool: semantic search (vector) across indexed entities (read-only). */
@Component
@RequiredArgsConstructor
public class SearchSemanticTool implements Tool {

    private final VectorSearchService vectorSearchService;

    @Override
    public String name() {
        return "search_semantic";
    }

    @Override
    public ToolResult execute(ToolCall call, ConversationContext ctx) {
        Object qRaw = call.arguments().get("query");
        if (qRaw == null || String.valueOf(qRaw).isBlank()) {
            return ToolResult.of("Please provide query.");
        }
        String query = String.valueOf(qRaw);

        int limit = 5;
        try {
            Object limitRaw = call.arguments().get("limit");
            if (limitRaw != null) limit = Math.min(10, Math.max(1, Integer.parseInt(String.valueOf(limitRaw))));
        } catch (Exception ignored) {
        }

        var hits = vectorSearchService.semanticSearch(ctx, query, limit, null, null, null);
        if (hits.isEmpty()) {
            return ToolResult.of("No semantic matches found.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Semantic matches:\n");
        for (var h : hits) {
            sb.append("- [").
                    append(h.entityType()).
                    append("] entityId=").
                    append(h.entityId()).
                    append(" score=").
                    append(h.score()).
                    append(" :: ").
                    append(h.content()).
                    append("\n");
        }
        return ToolResult.of(sb.toString().trim());
    }
}


