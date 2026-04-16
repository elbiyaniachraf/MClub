package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.dto.CommentCreateRequest;
import io.droidevs.mclub.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Tool: reply to an existing comment (child comment). */
@Component
@RequiredArgsConstructor
public class ReplyToCommentTool implements Tool {

    private final CommentService commentService;

    @Override
    public String name() {
        return "reply_comment";
    }

    @Override
    public ToolResult execute(ToolCall call, ConversationContext ctx) {
        String email = ctx.userEmail().orElseThrow(() -> new IllegalStateException("User not linked"));

        Object parentIdRaw = call.arguments().get("commentId");
        Object textRaw = call.arguments().get("text");

        if (parentIdRaw == null || textRaw == null) {
            return ToolResult.of("Please provide commentId and text.");
        }

        UUID parentId = UUID.fromString(String.valueOf(parentIdRaw));

        CommentCreateRequest req = new CommentCreateRequest();
        req.setContent(String.valueOf(textRaw));
        req.setParentId(parentId);

        commentService.reply(parentId, req, email);
        return ToolResult.of("Reply posted.");
    }
}


