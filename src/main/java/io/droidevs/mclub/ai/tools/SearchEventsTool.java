package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.ai.retrieval.vector.VectorSearchResult;
import io.droidevs.mclub.ai.retrieval.vector.VectorSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Tool: semantic search restricted to EVENT documents (read-only). */
@Component
@RequiredArgsConstructor
public class SearchEventsTool implements Tool {

    private final VectorSearchService vectorSearchService;

    @Override
    public String name() {
        return "search_events";
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

        List<VectorSearchResult> hits = vectorSearchService.semanticSearch(ctx, query, limit, "EVENT", null, null);
        if (hits.isEmpty()) return ToolResult.of("No events matched semantically.");

        StringBuilder sb = new StringBuilder();
        sb.append("Event matches (use id as eventId for actions):\n");
        int i = 1;
        var candidates = new java.util.ArrayList<java.util.Map<String, Object>>();
        for (var h : hits) {
            sb.append(i).append(") entityId=").append(h.entityId())
                    .append(" score=").append(h.score())
                    .append(" :: ").append(h.content())
                    .append("\n");

            candidates.add(java.util.Map.of(
                    "index", i,
                    "entityType", "EVENT",
                    "entityId", String.valueOf(h.entityId()),
                    "score", h.score(),
                    "snippet", h.content()
            ));
            i++;
        }

        return ToolResult.of(sb.toString().trim(), java.util.Map.of(
                "candidates", candidates
        ));
    }
}

