package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Tool: student check-in using the event QR rawToken (service enforces window + registration + idempotency). */
@Component
@RequiredArgsConstructor
public class CheckInEventTool implements Tool {

    private final AttendanceService attendanceService;

    @Override
    public String name() {
        return "checkin_event";
    }

    @Override
    public ToolResult execute(ToolCall call, ConversationContext ctx) {
        String email = ctx.userEmail().orElse(null);
        if (email == null) {
            return ToolResult.of("To check in, please link your account first (OTP linking). Ask me: 'link my account'.");
        }

        Object tokenRaw = call.arguments().get("qrToken");
        if (tokenRaw == null) {
            return ToolResult.of("To check in, please send the event QR token (qrToken).");
        }

        String rawToken = String.valueOf(tokenRaw).trim();
        if (rawToken.isBlank()) {
            return ToolResult.of("The QR token was empty. Please rescan and try again.");
        }

        try {
            var record = attendanceService.studentCheckInByEventQr(rawToken, email);
            return ToolResult.of("Checked in successfully for event " + record.getEventId() + ".");
        } catch (ResourceNotFoundException e) {
            return ToolResult.of("I couldn't find a valid check-in window for that QR token. Please rescan or ask an admin.");
        } catch (RuntimeException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            String lower = msg.toLowerCase();
            if (lower.contains("already") && lower.contains("check")) {
                return ToolResult.of("You're already checked in for this event.");
            }
            if (lower.contains("not registered")) {
                return ToolResult.of("You must be registered for the event before checking in.");
            }
            if (lower.contains("window") || lower.contains("time")) {
                return ToolResult.of("Check-in isn't available right now (outside the allowed time window).");
            }
            return ToolResult.of("I couldn't check you in: " + msg);
        }
    }
}
