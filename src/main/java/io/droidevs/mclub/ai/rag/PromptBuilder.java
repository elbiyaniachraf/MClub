package io.droidevs.mclub.ai.rag;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.conversation.ConversationSession;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/** Builder-pattern style prompt builder. */
@Component
public class PromptBuilder {

    public String buildPrompt(ConversationSession session, ConversationContext ctx, RetrievalContext retrieved) {
        int total = session.messages().size();
        int skip = Math.max(0, total - 30);
        String history = session.messages().stream()
                .skip(skip)
                .limit(30)
                .map(m -> m.role() + ": " + m.content())
                .collect(Collectors.joining("\n"));

        String facts = String.join("\n---\n", retrieved.factualSnippets());
        String recentEvents = retrieved.recentEvents().isEmpty()
                ? "(none)"
                : String.join("\n", retrieved.recentEvents());
        String semanticHits = retrieved.semanticHits().isEmpty()
                ? "(none)"
                : String.join("\n", retrieved.semanticHits());

        return """
                You are MClub Assistant.

                Style:
                - Be friendly, natural, and helpful.
                - Keep responses short, but ask a follow-up question when needed.

                Tool / safety rules:
                - NEVER guess UUIDs or database IDs.
                - Prefer scoped tools first when available:
                  - search_events_in_context / search_clubs_in_context
                  - then search_events / search_clubs / search_semantic
                - If a search tool returns a candidates list, do NOT copy UUIDs from text. Use pick_candidate with the 'candidates' array and the chosen index.
                - If multiple candidates exist, ask the user to choose (numbered list).
                - Do not claim you performed an action unless a tool call was executed.
                - If user is not linked, do not perform actions. Explain linking.

                User linked: %s

                Retrieved facts:
                %s

                Recent events snapshot:
                %s

                Semantic hits (vector search):
                %s

                Conversation:
                %s
                """.formatted(ctx.linked(), facts, recentEvents, semanticHits, history);
    }
}
