package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.ai.retrieval.vector.VectorSearchResult;
import io.droidevs.mclub.ai.retrieval.vector.VectorSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Tool: search clubs with auto-scoping.
 *
 * <p>If the current ConversationContext has clubScopeId, it returns that club as the only candidate.
 */
@Component
@RequiredArgsConstructor
public class SearchClubsInContextTool implements Tool {

    private final VectorSearchService vectorSearchService;

    @Override
    public String name() {
        return "search_clubs_in_context";
    }

    @Override
    public ToolResult execute(ToolCall call, ConversationContext ctx) {
        if (ctx != null && ctx.clubScopeId().isPresent()) {
            UUID clubId = ctx.clubScopeId().get();
            var candidate = java.util.Map.of(
                    "index", 1,
                    "entityType", "CLUB",
                    "entityId", String.valueOf(clubId),
                    "score", 1.0,
                    "snippet", "(current page club)"
            );
            return ToolResult.of(
                    "Club in current context:\n1) entityId=" + clubId,
                    java.util.Map.of("candidates", java.util.List.of(candidate))
            );
        }

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
        sb.append("Club matches (use id as clubId for actions):\n");
        int i = 1;
        var candidates = new java.util.ArrayList<java.util.Map<String, Object>>();
        for (var h : hits) {
            sb.append(i).append(") entityId=").append(h.entityId())
                    .append(" score=").append(h.score())
                    .append(" :: ").append(h.content())
                    .append("\n");

            candidates.add(java.util.Map.of(
                    "index", i,
                    "entityType", "CLUB",
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

