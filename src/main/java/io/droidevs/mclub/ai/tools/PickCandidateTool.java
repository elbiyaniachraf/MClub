package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Tool: pick a candidate from a previously returned candidates list.
 *
 * <p>This avoids the LLM copying UUIDs out of free-form text.
 */
@Component
public class PickCandidateTool implements Tool {

    @Override
    public String name() {
        return "pick_candidate";
    }

    @Override
    public ToolResult execute(ToolCall call, ConversationContext ctx) {
        Object candidatesRaw = call.arguments().get("candidates");
        Object indexRaw = call.arguments().get("index");

        if (!(candidatesRaw instanceof List<?> list) || list.isEmpty()) {
            return ToolResult.of("No candidates were provided to pick from.");
        }
        if (indexRaw == null) {
            return ToolResult.of("Please provide index (1..N) to pick a candidate.");
        }

        int index;
        try {
            index = Integer.parseInt(String.valueOf(indexRaw));
        } catch (Exception e) {
            return ToolResult.of("Invalid index. Use a number like 1, 2, 3.");
        }

        if (index < 1 || index > list.size()) {
            return ToolResult.of("Index out of range. Please pick between 1 and " + list.size() + ".");
        }

        Object item = list.get(index - 1);
        if (!(item instanceof Map<?, ?> m)) {
            return ToolResult.of("Candidate format was invalid.");
        }

        Object entityType = m.get("entityType");
        Object entityId = m.get("entityId");
        if (entityId == null) {
            return ToolResult.of("Selected candidate did not include an entityId.");
        }

        return ToolResult.of(
                "Picked candidate " + index + ": " + String.valueOf(entityType) + " id=" + String.valueOf(entityId),
                Map.of(
                        "entityType", entityType == null ? "" : String.valueOf(entityType),
                        "entityId", String.valueOf(entityId)
                )
        );
    }
}

