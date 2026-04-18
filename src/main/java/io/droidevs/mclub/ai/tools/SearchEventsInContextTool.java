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
 * Tool: search events with auto-scoping.
 *
 * <p>If the current ConversationContext has eventScopeId, it returns that event as the only candidate.
 * If it has clubScopeId, it searches EVENT documents filtered to that club.
 */
@Component
@RequiredArgsConstructor
public class SearchEventsInContextTool implements Tool {

    private final VectorSearchService vectorSearchService;

    @Override
    public String name() {
        return "search_events_in_context";
    }

    @Override
    public ToolResult execute(ToolCall call, ConversationContext ctx) {
        // Strongest scope: event
        if (ctx != null && ctx.eventScopeId().isPresent()) {
            UUID eventId = ctx.eventScopeId().get();
            var candidate = java.util.Map.of(
                    "index", 1,
                    "entityType", "EVENT",
                    "entityId", String.valueOf(eventId),
                    "score", 1.0,
                    "snippet", "(current page event)"
            );
            return ToolResult.of(
                    "Event in current context:\n1) entityId=" + eventId,
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

        UUID clubId = (ctx != null) ? ctx.clubScopeId().orElse(null) : null;
        List<VectorSearchResult> hits = vectorSearchService.semanticSearch(ctx, query, limit, "EVENT", clubId, null);
        if (hits.isEmpty()) return ToolResult.of("No events matched semantically in the current context.");

        StringBuilder sb = new StringBuilder();
        sb.append("Event matches in context (use id as eventId for actions):\n");
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
                "candidates", candidates,
                "clubScopeId", clubId == null ? null : String.valueOf(clubId)
        ));
    }
}

