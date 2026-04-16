package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.ai.retrieval.vector.VectorSearchResult;
import io.droidevs.mclub.ai.retrieval.vector.VectorSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Tool: semantic search restricted to CLUB documents (read-only). */
@Component
@RequiredArgsConstructor
public class SearchClubsTool implements Tool {

    private final VectorSearchService vectorSearchService;

    @Override
    public String name() {
        return "search_clubs";
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

        List<VectorSearchResult> hits = vectorSearchService.semanticSearch(ctx, query, limit, "CLUB", null, null);
        if (hits.isEmpty()) return ToolResult.of("No clubs matched semantically.");

        StringBuilder sb = new StringBuilder();
        sb.append("Club matches:\n");
        int i = 1;
        for (var h : hits) {
            sb.append(i++).append(") entityId=").append(h.entityId())
                    .append(" score=").append(h.score())
                    .append(" :: ").append(h.content())
                    .append("\n");
        }
        return ToolResult.of(sb.toString().trim());
    }
}

