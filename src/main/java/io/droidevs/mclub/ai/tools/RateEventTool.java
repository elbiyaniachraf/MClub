package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.dto.EventRatingRequest;
import io.droidevs.mclub.exception.ResourceNotFoundException;
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
        String email = ctx.userEmail().orElse(null);
        if (email == null) {
            return ToolResult.of("To rate events, please link your account first (OTP linking). Ask me: 'link my account'.");
        }

        Object eventIdRaw = call.arguments().get("eventId");
        Object starsRaw = call.arguments().get("stars");
        Object commentRaw = call.arguments().get("comment");

        if (eventIdRaw == null || starsRaw == null) {
            return ToolResult.of("Please provide eventId and stars (1-5).");
        }

        UUID eventId;
        try {
            eventId = UUID.fromString(String.valueOf(eventIdRaw));
        } catch (Exception e) {
            return ToolResult.of("Invalid eventId. Expected UUID.");
        }

        int stars;
        try {
            stars = Integer.parseInt(String.valueOf(starsRaw));
        } catch (Exception e) {
            return ToolResult.of("Invalid stars value. Use a number from 1 to 5.");
        }
        if (stars < 1 || stars > 5) {
            return ToolResult.of("Stars must be between 1 and 5.");
        }

        EventRatingRequest req = new EventRatingRequest();
        req.setRating(stars);
        req.setComment(commentRaw == null ? null : String.valueOf(commentRaw));

        try {
            eventRatingService.rateEvent(eventId, req, email);
            return ToolResult.of("Thanks. Your rating was saved for event " + eventId + ".");
        } catch (ResourceNotFoundException e) {
            return ToolResult.of("I can't find that event. Please double-check the id.");
        } catch (RuntimeException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            return ToolResult.of("I couldn't save your rating: " + msg);
        }
    }
}
