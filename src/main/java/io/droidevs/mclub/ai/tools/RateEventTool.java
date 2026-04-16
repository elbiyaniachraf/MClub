package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.dto.EventRatingRequest;
import io.droidevs.mclub.service.EventRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Tool: rate an event (stores/updates the user's latest rating). */
@Component
@RequiredArgsConstructor
public class RateEventTool implements Tool {

    private final EventRatingService eventRatingService;

    @Override
    public String name() {
        return "rate_event";
    }

    @Override
    public ToolResult execute(ToolCall call, ConversationContext ctx) {
        String email = ctx.userEmail().orElseThrow(() -> new IllegalStateException("User not linked"));

        Object eventIdRaw = call.arguments().get("eventId");
        Object starsRaw = call.arguments().get("stars");
        Object commentRaw = call.arguments().get("comment");

        if (eventIdRaw == null || starsRaw == null) {
            return ToolResult.of("Please provide eventId and stars (1-5).");
        }

        UUID eventId = UUID.fromString(String.valueOf(eventIdRaw));
        int stars = Integer.parseInt(String.valueOf(starsRaw));

        EventRatingRequest req = new EventRatingRequest();
        req.setRating(stars);
        req.setComment(commentRaw == null ? null : String.valueOf(commentRaw));

        eventRatingService.rateEvent(eventId, req, email);
        return ToolResult.of("Thanks. Your rating was saved for event " + eventId + ".");
    }
}


