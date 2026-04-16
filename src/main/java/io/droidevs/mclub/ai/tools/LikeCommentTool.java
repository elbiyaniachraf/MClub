package io.droidevs.mclub.ai.tools;

import io.droidevs.mclub.ai.conversation.ConversationContext;
import io.droidevs.mclub.ai.rag.ToolCall;
import io.droidevs.mclub.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Tool: like/unlike a comment. */
@Component
@RequiredArgsConstructor
public class LikeCommentTool implements Tool {

    private final CommentService commentService;

    @Override
    public String name() {
        return "toggle_like_comment";
    }

    @Override
    public ToolResult execute(ToolCall call, ConversationContext ctx) {
        String email = ctx.userEmail().orElseThrow(() -> new IllegalStateException("User not linked"));

        Object commentIdRaw = call.arguments().get("commentId");
        if (commentIdRaw == null) {
            return ToolResult.of("Please provide commentId.");
        }

        UUID commentId = UUID.fromString(String.valueOf(commentIdRaw));
        commentService.toggleLike(commentId, email);
        return ToolResult.of("Updated like.");
    }
}

