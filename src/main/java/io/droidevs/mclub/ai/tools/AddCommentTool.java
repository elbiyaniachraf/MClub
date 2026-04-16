package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.domain.CommentTargetType;
import io.droidevs.mclub.dto.CommentCreateRequest;
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
        String email = ctx.userEmail().orElseThrow(() -> new IllegalStateException("User not linked"));

        Object targetTypeRaw = call.arguments().get("targetType");
        Object targetIdRaw = call.arguments().get("targetId");
        Object textRaw = call.arguments().get("text");

        if (targetTypeRaw == null || targetIdRaw == null || textRaw == null) {
            return ToolResult.of("Please provide targetType (event/activity), targetId, and text.");
        }

        CommentTargetType type = CommentTargetType.valueOf(String.valueOf(targetTypeRaw).toUpperCase());
        UUID targetId = UUID.fromString(String.valueOf(targetIdRaw));

        CommentCreateRequest req = new CommentCreateRequest();
        req.setContent(String.valueOf(textRaw));
        req.setParentId(null);

        commentService.addComment(type, targetId, req, email);
        return ToolResult.of("Comment posted.");
    }
}


