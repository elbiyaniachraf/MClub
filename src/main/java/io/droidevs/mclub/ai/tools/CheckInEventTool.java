package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;
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
        String email = ctx.userEmail().orElseThrow(() -> new IllegalStateException("User not linked"));

        Object tokenRaw = call.arguments().get("qrToken");
        if (tokenRaw == null) {
            return ToolResult.of("To check in, please send the event QR token (qrToken).");
        }

        String rawToken = String.valueOf(tokenRaw);
        var record = attendanceService.studentCheckInByEventQr(rawToken, email);
        return ToolResult.of("Checked in successfully for event " + record.getEventId() + ".");
    }
}


