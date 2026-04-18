package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.domain.CommentTargetType;
import io.droidevs.mclub.dto.CommentCreateRequest;
import io.droidevs.mclub.exception.ResourceNotFoundException;
import io.droidevs.mclub.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Tool: add a top-level comment to an EVENT or ACTIVITY. */
@Component
@RequiredArgsConstructor
public class AddCommentTool implements Tool {

    private final CommentService commentService;

    @Override
    public String name() {
        return "add_comment";
    }

    @Override
    public ToolResult execute(ToolCall call, ConversationContext ctx) {
        String email = ctx.userEmail().orElse(null);
        if (email == null) {
            return ToolResult.of("To post comments, please link your account first (OTP linking). Ask me: 'link my account'.");
        }

        Object targetTypeRaw = call.arguments().get("targetType");
        Object targetIdRaw = call.arguments().get("targetId");
        Object textRaw = call.arguments().get("text");

        if (targetTypeRaw == null || targetIdRaw == null || textRaw == null) {
            return ToolResult.of("Please provide targetType (EVENT/ACTIVITY), targetId, and text.");
        }

        CommentTargetType type;
        try {
            type = CommentTargetType.valueOf(String.valueOf(targetTypeRaw).toUpperCase());
        } catch (Exception e) {
            return ToolResult.of("Invalid targetType. Use EVENT or ACTIVITY.");
        }

        UUID targetId;
        try {
            targetId = UUID.fromString(String.valueOf(targetIdRaw));
        } catch (Exception e) {
            return ToolResult.of("Invalid targetId. Expected UUID.");
        }

        CommentCreateRequest req = new CommentCreateRequest();
        req.setContent(String.valueOf(textRaw));
        req.setParentId(null);

        try {
            commentService.addComment(type, targetId, req, email);
            return ToolResult.of("Comment posted.");
        } catch (ResourceNotFoundException e) {
            return ToolResult.of("I couldn't find that item to comment on. Please check the id and try again.");
        } catch (RuntimeException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            return ToolResult.of("I couldn't post the comment: " + msg);
        }
    }
}
