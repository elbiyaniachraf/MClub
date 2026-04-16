package io.droidevs.mclub.ai.web;

import io.droidevs.mclub.ai.web.dto.ChatMessageRequest;
import io.droidevs.mclub.ai.web.dto.ChatMessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Platform chatbot endpoint (non-WhatsApp) that reuses the same RAG pipeline.
 *
 * <p>Client: web widget / mobile app / admin UI.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatWidgetController {

    private final PlatformChatService platformChatService;

    @PostMapping("/message")
    public ChatMessageResponse send(jakarta.servlet.http.HttpSession httpSession,
                                    @RequestBody @Valid ChatMessageRequest req) {
        String conversationId = (req.conversationId() == null || req.conversationId().isBlank())
                ? "web:" + httpSession.getId()
                : req.conversationId();

        var r = platformChatService.chat(conversationId, req.from(), req.text());
        return new ChatMessageResponse(r.message());
    }
}

